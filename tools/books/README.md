# The books

`gen_books.py` writes `HouseBooks.java` and `LabBooks.java` from `prose.py`.

Run it from the repo root:

    python3 tools/books/gen_books.py

## Why this exists

A Minecraft book page is about **114 pixels wide and 14 rows tall**, and
overflow is **silent** — the text is simply not drawn. Seven pages of the
hand-written set were losing their last lines, including the page that says
`HE IS NOT DEAD`.

`bind.py` lays prose out against the real glyph widths (`i` is 2px, `l` is 3,
`t` is 4, most letters 6, space 4) and paginates so no page can overflow. Write
paragraphs in `prose.py`; the pagination is not your problem.

Editing the generated Java by hand works, but nothing will warn you when a page
goes over. Prefer editing `prose.py` and re-running.

The generator is idempotent — running it twice is a no-op.
