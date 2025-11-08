open module net.silver.posman.tests {

  // 1. Core annotations and interfaces (your code compiles against this)
  requires org.junit.jupiter.api;

  // 2. The component that executes the tests (the missing module)
  requires org.junit.jupiter.engine;

  // 3. The launcher that discovers and starts the test platform (good practice)
  requires org.junit.platform.launcher;


}
