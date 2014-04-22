#!/usr/bin/python -O

import codecs

for line in codecs.open('languages.tsv', 'r', encoding='utf-8'):
    (id, langCode) = line.split('\t')[:2]
    if id != 'id':  # skip header
        v = langCode.upper().replace('-', '_')
        print('    public static final Language %s = Language.getByLangCode("%s");' % (v, langCode))

