#!/usr/bin/python -O

import sys

if sys.version_info < (3, 0):
    sys.stderr.write('this program requires python 3000 (its 10x faster under it)\n')
    sys.exit(1)

import hashlib
import codecs
import collections
import datetime
import json
import time
import traceback

def str_md5(s):
    foo = hashlib.md5()
    foo.update(s.encode('UTF-8'))
    return (foo.hexdigest())

def apply_diffs(initial_text, diffs):
    prev = initial_text
    i = 0
    cur = ''
    for diff in diffs:
        op = diff['op']
        loc = diff['loc']
        delta = diff['text']
        cur += prev[i:int(loc)]
        i = int(loc)
        if op == 'i':
            cur += delta
        else:
            assert(op == 'd')
            assert(prev[i:].startswith(delta))
            i += len(delta)
    cur += prev[i:]
    return cur

def main(stdin, stdout, valid_ids=None):
    last_page_id = None
    rev_index = 0
    text = None

    phrases = collections.defaultdict(int)
    
    for line in stdin:
        try:
            rev = json.loads(line.strip())
            if last_page_id != rev['pageId']:
                last_page_id = rev['pageId']
                text = None
            if 'text' in rev.keys():
                text = rev['text']
            else:
                assert(text != None)
                text = apply_diffs(text, rev['diffs'])
                if str_md5(text) != rev['md5']:
                    raise ValueError('invalid checksum in rev %s' %  rev['_id'])
                phrases[text] += 1
                if len(phrases) > 100000:
                    phrases = prune_counts(phrases, 10000)
                    print('pruned to length %d min %d...' % (len(phrases), min(phrases.values())))
                    show_top(phrases, 100)

        except:
            sys.stderr.write('decoding of line ' + repr(line) + ' failed:\n')
            traceback.print_exc()
            break
    show_top(phrases, 1000)

def prune_counts(counts, n):
    values = list(counts.values())
    values.sort()
    values.reverse()
    threshold = values[n]
    new_counts = collections.defaultdict(int)
    for (x, n) in counts.items():
        if n > threshold:
            new_counts[x] = n
    return new_counts


def show_top(counts, n):
    values = list(counts.values())
    values.sort()
    values.reverse()
    threshold = values[n]
    top = []
    for (x, n) in counts.items():
        if n >= threshold:
            top.append((n, x))
    top.sort()
    top.reverse()
    print('top %s phrases:' % n)
    for (i, (n, t)) in enumerate(top):
        t = t.replace('\n', ' ')[:50]
        print('%d. (n=%d,len=%d) %s' % (i+1, n, len(t), t))


if __name__ == '__main__':
    main(sys.stdin, sys.stdout)
