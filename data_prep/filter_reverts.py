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
        utils.GroupCounts('is self revert', lambda r: r.rev1.user_id == r.rev2.user_id),
        utils.GroupCounts('retain', lambda r: r.should_retain()),
    ]
    closed_ids = set()
    visited_ids = {}
    reader = utils.RevertReader(open(path_in), True)
    for revert in reader:
        for g in groups:
            g.count(revert)
        if revert.should_retain():
            (id1, id2) = (revert.rev1.id, revert.rev2.id)
            visited = False
            for id in (id1, id2):
                if id in visited_ids:
                    visited = True
                    closed_ids.add(visited_ids[id])
            if visited or id1 in closed_ids or id2 in closed_ids:
                closed_ids.add(id1)
                closed_ids.add(id2)
            else:
                visited_ids[id1] = id2
        num_lines += 1
    utils.warn('total reverts: %d' % num_lines)
    for g in groups:
        g.output(sys.stderr)

    utils.warn('\n\n\nafter filtering....')
    reader = utils.RevertReader(open(path_in), True)
    out = open(path_out, 'w')
    num_lines = 0
    for revert in reader:
        if revert.rev1.id in closed_ids or revert.rev2.id in closed_ids:
            out.write(reader.line)
            num_lines += 1
    out.close()
    utils.warn('total filitered reverts: %d' % num_lines)

def is_ip(s):
    return len(s.split('.')) == 4

if __name__ == '__main__':
    main('dat/reverts.txt', 'dat/filtered_reverts.txt')

