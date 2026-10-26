"""Catch catalogue/UI identity drift and ambiguous runtime metadata."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('sync_model_guide', ROOT / 'tools/sync_model_guide.py')
guide = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guide)


class ModelGuideContractTest(unittest.TestCase):
    def setUp(self):
        self.catalog = json.loads((ROOT / 'docs/model-catalog.json').read_text(encoding='utf-8'))

    def test_bundled_guide_binds_every_executable_slot_to_its_real_runtime(self):
        expected = guide.build(self.catalog)
        bundled = json.loads((ROOT / 'vision/src/main/assets/one/model-guide.json').read_text(encoding='utf-8'))
        self.assertEqual(expected, bundled)
        self.assertEqual(self.catalog['distinctExecutableIdCount'], len(bundled['models']))
        ids = {identity for values in self.catalog['featureSlots'].values() for identity in values}
        self.assertEqual(ids, set(bundled['models']))
        self.assertEqual('code_engine', bundled['models']['android-media']['kind'])
        self.assertEqual('code_engine', bundled['models']['zxing-qr']['kind'])
        self.assertNotIn('validation', bundled['models']['mobilenet-v4-fp32'])

    def test_duplicate_identity_cannot_silently_replace_another_runtime(self):
        altered = copy.deepcopy(self.catalog)
        altered['models'].append(copy.deepcopy(altered['models'][0]))
        with self.assertRaisesRegex(ValueError, 'Duplicate execution ID'):
            guide.build(altered)

    def test_feature_cannot_point_to_an_unrelated_model(self):
        altered = copy.deepcopy(self.catalog)
        altered['featureSlots']['classify'].append('zxing-qr')
        with self.assertRaisesRegex(ValueError, 'Unbound model/feature'):
            guide.build(altered)

    def test_missing_runtime_fails_instead_of_inventing_an_accelerator(self):
        altered = copy.deepcopy(self.catalog)
        altered['models'][0]['runtime'] = ' '
        with self.assertRaisesRegex(ValueError, 'Missing title/runtime'):
            guide.build(altered)


if __name__ == '__main__':
    unittest.main()
