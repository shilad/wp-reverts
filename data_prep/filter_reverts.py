#!/usr/bin/python

import utils
import collections
import string
import sys

def main(path_in, path_out):
    num_lines = 0
    groups = [
        utils.GroupCounts('is bot', lambda r: r.is_bot()),
        utils.GroupCounts('num_anon', lambda r: ((1 if r.rev1.is_anon() else 0)
                                         + (1 if r.rev2.is_anon() else 0))),
        utils.GroupCounts('is short', lambda r: r.is_short()),
        utils.GroupCounts('retain', lambda r: r.should_retain()),
    ]
    out = open(path_out, 'w')
    reader = utils.RevertReader(open(path_in), True)
    for revert in reader:
        for g in groups:
            g.count(revert)
        if revert.should_retain():
            out.write(reader.line)
        num_lines += 1
    utils.warn('total reverts: %d' % num_lines)
    for g in groups:
        g.output(sys.stderr)
    out.close()

def is_ip(s):
    return len(s.split('.')) == 4

if __name__ == '__main__':
    main('dat/reverts.txt', 'dat/filtered_reverts.txt')

