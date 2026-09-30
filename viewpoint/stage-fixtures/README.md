# Synthetic native-stage fixtures

These small test doubles were authored for this project. They contain no copied/decompiled game or mod implementation and perform no OpenGL operations. Names and signatures match the adapter's checked integration points so the production transformer, method-handle bootstrap and backend can execute together in an isolated classloader.

Their counters and injected failures establish adapter control flow, not real renderer behavior. The independent copied-binary test verifies the actual pinned targets without initialization. Fixtures are compiled into a separate test directory and never included in the core JAR or a mod package.
