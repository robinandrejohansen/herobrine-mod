"""Lay prose out on a Minecraft book page, the way the game actually does it."""
PAGE_PX, ROWS = 114, 14

W = {}
for c in map(chr, range(32, 127)):
    W[c] = 6
for c, w in {' ':4, '!':2, '.':2, ',':2, ':':2, ';':2, '|':2, "'":3, 'i':2,
             'l':3, 't':4, 'f':5, 'k':5, 'I':4, '[':4, ']':4, '(':5, ')':5,
             '{':5, '}':5, '"':5, '<':5, '>':5, '`':3, '*':5}.items():
    W[c] = w

def px(s):        return sum(W.get(c, 6) for c in s)

def wrap(par):
    """One paragraph -> the lines Minecraft will draw."""
    out, line = [], ""
    for word in par.split():
        trial = word if not line else line + " " + word
        if px(trial) <= PAGE_PX:
            line = trial
        else:
            if line: out.append(line)
            line = word
    if line: out.append(line)
    return out

def bind(prose, rows=ROWS):
    """Paragraphs (blank-line separated) -> pages that fit."""
    pages, page = [], []
    for par in [p.strip() for p in prose.strip().split("\n\n") if p.strip()]:
        block = wrap(par)
        need = len(block) + (1 if page else 0)
        if page and len(page) + need > rows:
            pages.append(page); page = []
            need = len(block)
        if page: page.append("")
        if len(block) > rows:                       # a paragraph too big for any page
            while block:
                pages.append(block[:rows]); block = block[rows:]
            continue
        page += block
    if page: pages.append(page)
    return ["\n".join(p) for p in pages]

def show(prose, label=""):
    pg = bind(prose)
    print("── %s : %d page(s)" % (label, len(pg)))
    for i, p in enumerate(pg, 1):
        n = p.count("\n") + 1
        print("   [%d] %2d/%d rows %s" % (i, n, ROWS, "" if n <= ROWS else "!! OVER"))
    return pg
