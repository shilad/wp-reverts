#!/usr/bin/python

import collections
import string
import sys

def warn(message):
    sys.stderr.write(message + '\n')

class Revision(object):
    __slots__ = ['id', 'tstamp', 'user_id', 'user_name', 'comment']
    def __init__(self, id, tstamp, user_id, user_name, comment):
        self.id = id
        self.tstamp = tstamp
        self.user_id = user_id
        self.user_name = user_name
        self.comment = comment

    def is_anon(self):
        return self.user_name == 'null'

    def is_bot(self):
        return 'bot' in self.user_name.lower()


class Revert(object):
    __slots__ = ['page_id', 'page_title', 'code', 'rev1', 'rev2']
    def __init__(self, page_id, page_title, code, rev1, rev2):
        self.page_id = page_id
        self.page_title = page_title
        self.code = code
        self.rev1 = rev1
        self.rev2 = rev2

    def is_bot(self):
        return 'b' in self.code or self.rev1.is_bot() or self.rev2.is_bot()

    def is_vloose(self):
        return 'vl' in self.code

    def is_vstrict(self):
        return 'vs' in self.code

    def is_fingerprint(self):
        return 'f' in self.code

    def is_short(self):
        l = len(self.code)
        return self.code[l/2] == 's' and self.code[:l] == self.code[-l:]

    def should_retain(self):
        return ((not self.rev1.is_anon() and not self.rev2.is_anon())
        and     (not self.is_bot())
        and     (not self.is_vstrict())
        and     (self.rev1.user_id != self.rev2.user_id)
        and     (not (self.is_fingerprint() and self.is_short())))

class GroupCounts:
    def __init__(self, caption, keyfn, filterfn=None):
        self.caption = caption
        self.keyfn = keyfn
        self.filterfn = filterfn
        self.counts = collections.defaultdict(int)

    def count(self, revert):
        if not self.filterfn or self.filterfn(revert):
            self.counts[self.keyfn(revert)] += 1

    def clear(self):
        self.counts.clear()

    def output(self, stream):
        total = sum(self.counts.values())
        items = self.counts.items()
        items.sort(key=lambda i: i[1])
        stream.write(self.caption + '\n')
        for (key, n) in items:
            stream.write('\t%s\t%d\t%.1f%%\n' % (key, n, 100.0*n/total))

class RevertReader:
    def __init__(self, file, show_status=False, target_ids=None):
        self.file = file
        self.line = None
        self.line_num = 0
        self.target_ids = target_ids
        self.show_status = show_status

    def __iter__(self):
        return self 
        
    def next(self):
        while True:
            line = self.file.readline()
            if not line:
                raise StopIteration
            self.line = line
        
            self.line_num += 1
            if self.show_status and self.line_num % 100000 == 0:
                warn('reading revert %d' % self.line_num)

            if line and line[-1] == '\n':
                line = line[:-1]
            if line and line[-1] == '\t':
                line = line[:-1]

            tokens = line.split('\t')
            if len(tokens) != 10:
                warn('invalid line in reverts: %s (error 1)' % line)
                continue

            page_id, page_name = tokens[0].split('@', 1)
            code = tokens[1]
            rev1 = self.fields_to_rev(tokens[2:6])
            rev2 = self.fields_to_rev(tokens[6:10])
            return Revert(page_id, page_name, code, rev1, rev2)

    def fields_to_rev(self, fields):
        (uid, uname) = fields[2].split('@', 1)
        return Revision(fields[0], fields[1], uid, uname, fields[3])
    
    
def main(path):
    num_lines = 0
    groups = [
        GroupCounts('is bot', lambda r: r.is_bot()),
        GroupCounts('num_anon', lambda r: ((1 if r.rev1.is_anon() else 0)
                                         + (1 if r.rev2.is_anon() else 0))),
        GroupCounts('is short', lambda r: r.is_short()),
        GroupCounts('retain', lambda r: r.should_retain()),
    ]
    for line in open(path):
        tokens = line.split('\t')
        page_id, page_name = tokens[0].split('@', 1)
        code = tokens[1]
        rev1 = fields_to_rev(tokens[2:6])
        rev2 = fields_to_rev(tokens[6:10])
        revert = Revert(page_id, page_name, code, rev1, rev2)
        for g in groups:
            g.count(revert)
        num_lines += 1
        if revert.should_retain():
            print revert.rev1.comment
        if num_lines % 100000 == 0:
            for g in groups:
                g.output(sys.stderr)

def output_counts(counts):
    total = sum(counts.values())
    print 'for total', total
    for (key, count) in counts.items():
        print '\t', key, count, '%.3f%%' % (100.0 * count / total)

if __name__ == '__main__':
    main('dat/reverts.txt')

