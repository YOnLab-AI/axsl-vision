#!/usr/bin/env python3
"""Build the small in-app guide from reviewed catalogue metadata, without weights."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/model-catalog.json'
OUTPUT = ROOT / 'vision/src/main/assets/one/model-guide.json'


def build(catalog: dict) -> dict:
    if catalog.get('schema') != 'axsl-vision-model-catalog-v1':
        raise ValueError('Unsupported model catalogue schema')
    models = {}
    for source in catalog['models']:
        identity = source['id']
        if identity in models:
            raise ValueError('Duplicate execution ID: ' + identity)
        row = {field: source[field] for field in ('title', 'kind', 'runtime', 'features')}
        if not row['title'].strip() or not row['runtime'].strip():
            raise ValueError('Missing title/runtime: ' + identity)
        if row['kind'] not in ('code_engine', 'trained_pipeline'):
            raise ValueError('Unsupported execution kind: ' + identity)
        if not isinstance(row['features'], list) or not row['features']:
            raise ValueError('Missing feature bindings: ' + identity)
        models[identity] = row
    slots = catalog['featureSlots']
    for feature, identities in slots.items():
        for identity in identities:
            if identity not in models or feature not in models[identity]['features']:
                raise ValueError('Unbound model/feature: ' + feature + '/' + identity)
    return {
        'schema': 'axsl-vision-in-app-model-guide-v1',
        'source': 'docs/model-catalog.json',
        'scope': 'Execution identity and runtime metadata; no benchmark or accuracy claims',
        'models': dict(sorted(models.items())),
    }


def render(document: dict) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2) + '\n'


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='fail if bundled metadata drifted')
    arguments = parser.parse_args()
    expected = render(build(json.loads(SOURCE.read_text(encoding='utf-8'))))
    if arguments.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(encoding='utf-8') != expected:
            print('Model guide differs from the reviewed catalogue; run tools/sync_model_guide.py')
            return 1
        print('In-app model guide matches the catalogue')
    else:
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_text(expected, encoding='utf-8', newline='\n')
        print('Generated ' + OUTPUT.relative_to(ROOT).as_posix())
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
