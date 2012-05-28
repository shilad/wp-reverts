#!/usr/bin/python -O
#
# Usage:
#

import sys

retained_ids = set([s.strip() for s in open('dat/top_2M_ids.txt')])

for line in sys.stdin:
    (doc_id, code, sims) = line.split()
    if doc_id not in retained_ids:
        continue
    sys.stdout.write(doc_id + ' ')
    features = []
    for pair in sims.split('|'):
        doc_id2, score = pair.split(',')
        if doc_id2 in retained_ids:
            features.append((int(doc_id2), score))
            if len(features) >= 2000:
                break
    features.sort()
    tokens = ['%s:%s' % pair for pair in features]
    sys.stdout.write(' '.join(tokens))
    sys.stdout.write('\n')
