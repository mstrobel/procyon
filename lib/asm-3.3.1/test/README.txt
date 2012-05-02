This directory contains the tests of the product. 
It contains(*) the following items:

- lib: external libraries required to run the tests
- conform: conformance tests (unit tests)
- deviance: deviance tests (unit tests)
- thread: multi-threading tests (unit tests)
- stress: stress tests
- perf: performance tests

Each sub directory contains:
- the source of the tests, with package struture if there is one,
- the xml descriptors to launch test suites. An xml desriptor describes
  the execution of a test suite. The default task is called on this file.

(*) some items may not be present, depending on the product.