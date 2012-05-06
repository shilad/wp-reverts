#!/usr/bin/python -O

import sys

if sys.version_info < (3, 0):
    sys.stderr.write('this program requires python 3000 (its 10x faster under it)\n')
    sys.exit(1)

import codecs
import collections
import datetime
import json
import time
import traceback

def main(stdin, stdout):
    last_page_id = None
    num_revs = 0
    
    counts = collections.defaultdict(int)
    md5s = []
    for line in stdin:
        try:
            obj = json.loads(line.strip())
            if obj['totalBytes'] < 50:
                continue
            md5 = obj['md5']
            try:
                i = md5s.index(md5)
            except ValueError:
                counts[None] += 1
                pass
            else:
                counts[i] += 1
            md5s.insert(0, md5)
            if len(md5s) > 100:
                del(md5s[-1])
            num_revs += 1
            if num_revs % 10000 == 0:
                print_counts(counts)
        except:
            sys.stderr.write('decoding of line ' + repr(line) + ' failed:\n')
            traceback.print_exc()

def print_counts(counts):
    n = sum(counts.values())
    sys.stdout.write('n=%d none=%d' % (n, counts[None]))
    n -= counts[None]
    for i in range(20):
        sys.stdout.write(' %d=%d (%.2f%%)' % (i, counts.get(i, 0), 100.0*counts.get(i, 0)/n))
    sys.stdout.write('\n')

if __name__ == '__main__':
    main(sys.stdin, sys.stdout)
