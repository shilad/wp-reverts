#!/usr/bin/python -O

import sys

sizes = []
for line in sys.stdin:
    sizes.append(len(line.split()))
sizes.sort()

print 'n=%d, mean=%.1f' % (len(sizes), 1.0 * sum(sizes) / len(sizes))
print 'min=%.1f max=%.1f' % (min(sizes), max(sizes))
for p in [1,2,3,4,5,10,20,30,40,50,60,70,80,90,95,96,97,98,99]:
    i = int(len(sizes) * p / 100.0)
    print '%d%%iles: %.1f' % (p, sizes[i])
    
