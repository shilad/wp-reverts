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
    md5s = []
    
    for line in stdin:
        try:
            rev = json.loads(line.strip())
            if ':' in rev['title']:
                continue
            if last_page_id != rev['pageId']:
                last_page_id = rev['pageId']
                text = None
            if 'text' in rev.keys():
                text = rev['text']
            else:
                assert(text != None)
                text = apply_diffs(text, rev['diffs'])
                md5 = str_md5(text)
                if md5 != rev['md5']:
                    raise ValueError('invalid checksum in rev %s' %  rev['_id'])
                if len(text) <= 60 and md5 in md5s and not 'redirect' in text.lower():
                    print('short revert for %s' % repr(text))
                md5s.append(md5)
                if len(md5s) > 5:
                    del(md5s[0])
        except:
            sys.stderr.write('decoding of line ' + repr(line) + ' failed:\n')
            traceback.print_exc()
            break

if __name__ == '__main__':
    main(sys.stdin, sys.stdout)
