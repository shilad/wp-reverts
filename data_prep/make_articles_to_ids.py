#!/usr/bin/python -O

import codecs
import sys

ids = set([line.strip() for line in open('dat/top_2M_ids.txt')])

f = codecs.open('dat/filtered_articles_to_ids.txt', 'w', encoding='UTF-8')
for line in codecs.open('dat/articles_to_ids.txt', encoding='UTF-8'):
    try:
        (title, id) = line.split('\t')
        id = id.strip().encode('ASCII')
        if id in ids:
            f.write('%s\t%s\n' % (title, id))
    except:
        sys.stderr.write('invalid line: %s\n' % `line.encode('UTF-8')`)
f.close()
