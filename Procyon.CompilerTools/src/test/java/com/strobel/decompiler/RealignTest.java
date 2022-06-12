package com.strobel.decompiler;

import org.junit.Test;

public class RealignTest extends DecompilerTest {

    static class Test1 {
        
        public void method1() {
            System.out.println("Test.method1");
        }
        
        public void method2() {
            System.out.println("Test.method2");
        }
        
        class Inner1 {
            
            public Inner1() {
                System.out.println("Inner1 constructor");
            }
            
            public void method1() {
                System.out.println("Inner1.method1");
            }
            
            public void method2() {
                System.out.println("Inner1.method2");
            }
        }

        class Inner2 {
            
            public Inner2() {
                System.out.println("Inner2 constructor");
            }
        }


        public void method3() {
            System.out.println("Test.method3");
        }
        
        public void method4() {
            System.out.println("Test.method4");
        }

        class Inner3 {
            
            public Inner3() {
                System.out.println("Inner3 constructor");
            }
            
            public void method1() {
                System.out.println("Inner3.method1");
            }
            
            public void method2() {
                System.out.println("Inner3.method2");
            }
        }

        class Inner4 {
            
            public Inner4() {
                System.out.println("Inner4 constructor");
            }
            
            public void method1() {
                System.out.println("Inner4.method1");
            }
            
            public void method2() {
                System.out.println("Inner4.method2");
            }
        }

        public void method5() {
            System.out.println("Test.method5");
        }
        
        public void method6() {
            System.out.println("Test.method6");
        }
    }
    
    static class Test2 {

        private Test2 top = new Test2(0);
        private Test2 test;

        static {
            System.out.println("clinit1");
        }

        public Test2(int i) {
            System.out.println(i);
        }

        private Test2 middle = new Test2(false);

        public Test2(boolean flag) {
            System.out.println(flag);
            test = new Test2(0); // must not be moved to declaration
        }

        static {
            System.out.println("clinit2");
        }

        private Test2 bottom = new Test2(true);

        static {
            System.out.println("clinit3");
        }
    } 
    
    static class Test3 {
        
        private Test3 top = new Test3(0);
        private Test3 test;
        
        static {
            System.out.println("clinit1");
        }
        
        public Test3(int i) {
            this(false);
            System.out.println(i);
        }
        
        private Test3 middle = new Test3(false);
        
        public Test3(boolean flag) {
            System.out.println(flag);
            test = new Test3(0); // can be moved to declaration (because of this(...) call)
        }
        
        static {
            System.out.println("clinit2");
        }
        
        private Test3 bottom = new Test3(true);
        
        static {
            System.out.println("clinit3");
        }
    } 
    
    @Test
    public void testReOrderMembers() throws Throwable {
        verifyOutput(
            Test1.class,
            lineNumberSettings(),
            "static class Test1 {\n" +
            "    public void method1() {\n" +
            "        System.out.println(/*EL:10*/\"Test.method1\");\n" +
            "    }\n" +
            "    \n" +
            "    public void method2() {\n" +
            "        System.out.println(/*EL:14*/\"Test.method2\");\n" +
            "    }\n" +
            "    \n" +
            "    class Inner1 {\n" +
            "        public Inner1() {\n" +
            "            System.out.println(/*EL:20*/\"Inner1 constructor\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method1() {\n" +
            "            System.out.println(/*EL:24*/\"Inner1.method1\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method2() {\n" +
            "            System.out.println(/*EL:28*/\"Inner1.method2\");\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    class Inner2 {\n" +
            "        public Inner2() {\n" +
            "            System.out.println(/*EL:35*/\"Inner2 constructor\");\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    public void method3() {\n" +
            "        System.out.println(/*EL:41*/\"Test.method3\");\n" +
            "    }\n" +
            "    \n" +
            "    public void method4() {\n" +
            "        System.out.println(/*EL:45*/\"Test.method4\");\n" +
            "    }\n" +
            "    \n" +
            "    class Inner3 {\n" +
            "        public Inner3() {\n" +
            "            System.out.println(/*EL:51*/\"Inner3 constructor\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method1() {\n" +
            "            System.out.println(/*EL:55*/\"Inner3.method1\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method2() {\n" +
            "            System.out.println(/*EL:59*/\"Inner3.method2\");\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    class Inner4 {\n" +
            "        public Inner4() {\n" +
            "            System.out.println(/*EL:66*/\"Inner4 constructor\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method1() {\n" +
            "            System.out.println(/*EL:70*/\"Inner4.method1\");\n" +
            "        }\n" +
            "    \n" +
            "        public void method2() {\n" +
            "            System.out.println(/*EL:74*/\"Inner4.method2\");\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    public void method5() {\n" +
            "        System.out.println(/*EL:79*/\"Test.method5\");\n" +
            "    }\n" +
            "    \n" +
            "    public void method6() {\n" +
            "        System.out.println(/*EL:83*/\"Test.method6\");\n" +
            "    }\n" +
            "}"
        );
    }
    
    
    @Test
    public void testRewriteInit() throws Throwable {
        verifyOutput(
            Test2.class,
            lineNumberSettings(),
            "static class Test2 {\n" +
            "    /*SL:89*/private Test2 top = new Test2(0);\n" +
            "    static {\n" +
            "        System.out.println(/*EL:93*/\"clinit1\");\n" +
            "    }\n" +
            "    \n" +
            "    public Test2(final int i) {\n" +
            "        System.out.println(/*EL:97*/i);\n" +
            "    }\n" +
            "    \n" +
            "    /*SL:100*/private Test2 middle = new Test2(false);\n" +
            "    \n" +
            "    public Test2(final boolean flag) {\n" +
            "        System.out.println(/*EL:103*/flag);\n" +
            "        /*SL:104*/this.test = new Test2(0);\n" +
            "    }\n" +
            "    \n" +
            "    private Test2 test;\n" +
            "    static {\n" +
            "        System.out.println(/*EL:108*/\"clinit2\");\n" +
            "    }\n" +
            "    /*SL:111*/private Test2 bottom = new Test2(true);\n" +
            "    static {\n" +
            "        System.out.println(/*EL:114*/\"clinit3\");\n" +
            "    }\n" +
            "}"
        );
    }
    
    @Test
    public void testRewriteInit2() throws Throwable {
        verifyOutput(
                Test3.class,
                lineNumberSettings(),
                "static class Test3 {\n" +
                        "    /*SL:120*/private Test3 top = new Test3(0);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:124*/\"clinit1\");\n" +
                        "    }\n" +
                        "    \n" +
                        "    public Test3(final int i) {\n" +
                        "        /*SL:128*/this(false);\n" +
                        "        System.out.println(/*EL:129*/i);\n" +
                        "    }\n" +
                        "    \n" +
                        "    /*SL:132*/private Test3 middle = new Test3(false);\n" +
                        "    \n" +
                        "    public Test3(final boolean flag) {\n" +
                        "        System.out.println(/*EL:135*/flag);\n" +
                        "    }\n" +
                        "    /*SL:136*/private Test3 test = new Test3(0);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:140*/\"clinit2\");\n" +
                        "    }\n" +
                        "    /*SL:143*/private Test3 bottom = new Test3(true);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:146*/\"clinit3\");\n" +
                        "    }\n" +
                        "}"
                );
    }
    
    private static DecompilerSettings lineNumberSettings() {
        DecompilerSettings lineNumberSettings = defaultSettings();
        lineNumberSettings.setShowDebugLineNumbers(true);
        return lineNumberSettings;
    }
}
