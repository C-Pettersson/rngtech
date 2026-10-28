"""Merge RNGTech's FTB Quests chapters into an existing pack's quest book.

Usage: python merge_ftbquests.py <rngtech quests dir> <pack quests dir>

The RNGTech quests dir is extras/ftbquests/config/ftbquests/quests from this repository, or the same folder
from the rngtech-ftbquests release zip. The pack quests dir is the pack's config/ftbquests/quests. The script
copies the RNGTech chapters and reward tables, appends the RNGTech chapter group, and merges the en_us lang
entries. It stops without writing anything if a file name, quest object id, or lang key already exists in the pack.
"""
import re
import shutil
import sys
from pathlib import Path

ID = re.compile(r'\b[0-9A-F]{16}\b')
LANG_KEY = re.compile(r'^\t([^\s:]+):', re.MULTILINE)


def ids_in(folder):
    found = set()
    for path in folder.rglob('*.snbt'):
        if path.parent.name != 'lang':
            found.update(ID.findall(path.read_text(encoding='utf-8')))
    return found


def lang_body(text):
    start, end = text.index('{'), text.rindex('}')
    return text[start + 1:end].strip('\n')


def main(source, target):
    problems = []
    copies = []
    for sub in ('chapters', 'reward_tables'):
        for path in sorted((source / sub).glob('*.snbt')):
            dest = target / sub / path.name
            if dest.exists():
                problems.append(f'{sub}/{path.name} already exists in the pack')
            copies.append((path, dest))

    clashes = ids_in(source) & ids_in(target)
    if clashes:
        problems.append(f'object ids already used by the pack: {", ".join(sorted(clashes))}')

    source_lang = (source / 'lang' / 'en_us.snbt').read_text(encoding='utf-8')
    target_lang_path = target / 'lang' / 'en_us.snbt'
    target_lang = target_lang_path.read_text(encoding='utf-8')
    duplicate_keys = set(LANG_KEY.findall(source_lang)) & set(LANG_KEY.findall(target_lang))
    if duplicate_keys:
        problems.append(f'lang keys already in the pack: {", ".join(sorted(duplicate_keys)[:10])}')

    if problems:
        print('Nothing merged:')
        for problem in problems:
            print(f'  - {problem}')
        return 1

    for path, dest in copies:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)

    groups_path = target / 'chapter_groups.snbt'
    groups = groups_path.read_text(encoding='utf-8')
    source_groups = re.findall(r'\{ id: "([0-9A-F]{16})" \}', (source / 'chapter_groups.snbt').read_text(encoding='utf-8'))
    entries = ''.join(f'\t\t{{ id: "{group}" }}\n' for group in source_groups)
    close = groups.rindex(']')
    line_start = groups.rindex('\n', 0, close) + 1
    groups_path.write_text(groups[:line_start] + entries + groups[line_start:], encoding='utf-8')

    close = target_lang.rindex('}')
    merged = target_lang[:close].rstrip('\n') + '\n' + lang_body(source_lang) + '\n}\n'
    target_lang_path.write_text(merged, encoding='utf-8')

    print(f'Merged {len(copies)} files, {len(source_groups)} chapter group, and the en_us lang entries.')
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(Path(sys.argv[1]), Path(sys.argv[2])))
