import sys

def handle_event(line):
    segments = line.split()
    date, time = segments[0], segments[1]
    id, p, p2, ri = segments[6], int(segments[14]), int(segments[16]), int(segments[18])
    onset = segments[22] == 'true'
    count = 0
    if p == ri:
        count += 1
    if p2 == ri:
        count += 1
    if p == p2:
        count += 1
    scores = {}
    scores[ri] = scores.get(ri, 0) + 4
    if onset:
        scores[ri] = scores.get(ri, 0) + 4
    scores[p] = scores.get(p, 0) + 3
    scores[p2] = scores.get(p2, 0) + 3
    max_score = 0
    max_index = None
    for k,v in scores.items():
        if v > max_score:
            max_score = v
            max_index = k
    print date, time, id, '[' + str(count) + ']', ri == p, ri == p2, p == p2, p, p2, ri, onset, max_index, max_score

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: weighted_bi_finder.py <file>'
        sys.exit(1)
    with open(sys.argv[1], 'r') as f:
        for line in f:
            if 'Secondary' in line:
                handle_event(line.strip())


            
            
