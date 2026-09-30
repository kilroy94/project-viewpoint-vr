import tempfile
from pathlib import Path
import unittest
from inspect_contract import method_block, ordered_calls, pinned_copy
from verify_init_log import verify


class ContractTest(unittest.TestCase):
    def test_initialization_log(self):
        safe = "Initializing 'java/lang/Object'\nStart class verification for: viewpoint.SceneDrawer\n"
        verify(safe)
        for package in ["viewpoint/SceneDrawer", "zombie/GameWindow", "me/zed_0xff/zombie_buddy/Loader"]:
            with self.assertRaises(ValueError):
                verify(safe + "Initializing '" + package + "'\n")
        with self.assertRaises(ValueError):
            verify("")

    def test_descriptor_and_ambiguity(self):
        text = "  private void draw();\n    descriptor: ()V\n    Code:\n"
        self.assertEqual(method_block(text, "private void draw();", "()V"), text)
        for source, desc in [(text, "()Z"), (text + text, "()V"), ("", "()V")]:
            with self.assertRaises(ValueError):
                method_block(source, "private void draw();", desc)

    def test_invocations(self):
        self.assertEqual(ordered_calls("// Method a:()V\n// Method b:()V", ["a:()V", "b:()V"]), 2)
        for source in ["// Method b:()V\n// Method a:()V", "// Method a:()V",
                       "// Method a:()V\n// Method a:()V\n// Method b:()V"]:
            with self.assertRaises(ValueError):
                ordered_calls(source, ["a:()V", "b:()V"])

    def test_unknown_binary_does_not_copy(self):
        with tempfile.TemporaryDirectory() as folder:
            source, dest = Path(folder) / "source", Path(folder) / "dest"
            source.write_bytes(b"unsupported")
            with self.assertRaises(ValueError):
                pinned_copy(source, dest, ["0" * 64])
            self.assertFalse(dest.exists())


if __name__ == "__main__":
    unittest.main()
