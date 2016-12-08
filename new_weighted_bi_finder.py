import sys

def get_highest_ranked(id, all_lines):
    index = 0
    found = False
    for line in all_lines:
        if not found and id in line and 'Relative importance metrics' in line:
            found = True
        elif found:
            if '[ 1]' in line:
                return index
            else:
                index += 1
    raise Exception('Failed to find highest ranked API call')

def handle_event(line, all_lines):
    segments = line.split()
    date, time = segments[0], segments[1]
    id, p, p2, ri = segments[6], int(segments[14]), int(segments[16]), int(segments[18])
    ri_top = get_highest_ranked(id, all_lines)
    onset = segments[22] == 'true'
    scores = {}
    scores[ri_top] = scores.get(ri_top, 0) + 4
    if onset:
        scores[ri] = scores.get(ri, 0) + 3
    scores[p] = scores.get(p, 0) + 3
    scores[p2] = scores.get(p2, 0) + 3
    max_score = 0
    max_index = None
    for k,v in scores.items():
        if v > max_score:
            max_score = v
            max_index = k
    print date, time, id, ri_top, ri, p, p2, onset, max_index, max_score

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: weighted_bi_finder.py <file>'
        sys.exit(1)
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()
    print 'Date Time ID RI_Top RI_Inc P1 P2 Onset Bottleneck Score'
    for line in lines:
        if 'Secondary' in line:
            handle_event(line.strip(), lines)


            
            
