package com.strobel.decompiler;

import org.junit.Test;

public class RealignTest extends DecompilerTest {

    static class ReOrderMembers {
        
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
    
    static class RewriteInit {

        private RewriteInit top = new RewriteInit(0);
        private RewriteInit test;

        static {
            System.out.println("clinit1");
        }

        public RewriteInit(int i) {
            System.out.println(i);
        }

        private RewriteInit middle = new RewriteInit(false);

        public RewriteInit(boolean flag) {
            System.out.println(flag);
            test = new RewriteInit(0); // must not be moved to declaration
        }

        static {
            System.out.println("clinit2");
        }

        private RewriteInit bottom = new RewriteInit(true);

        static {
            System.out.println("clinit3");
        }
    } 
    
    static class RewriteInit2 {
        
        private RewriteInit2 top = new RewriteInit2(0);
        private RewriteInit2 test;
        
        static {
            System.out.println("clinit1");
        }
        
        public RewriteInit2(int i) {
            this(false);
            System.out.println(i);
        }
        
        private RewriteInit2 middle = new RewriteInit2(false);
        
        public RewriteInit2(boolean flag) {
            System.out.println(flag);
            test = new RewriteInit2(0); // can be moved to declaration (because of this(...) call)
        }
        
        static {
            System.out.println("clinit2");
        }
        
        private RewriteInit2 bottom = new RewriteInit2(true);
        
        static {
            System.out.println("clinit3");
        }
        
        class Inner {
            Inner() {
                System.out.println("Inner");
            }
        }
    } 
    
    @Test
    public void testReOrderMembers() throws Throwable {
        verifyOutput(
            ReOrderMembers.class,
            lineNumberSettings(),
            "static class ReOrderMembers {\n" +
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
            RewriteInit.class,
            lineNumberSettings(),
            "static class RewriteInit {\n" +
            "    /*SL:89*/private RewriteInit top = new RewriteInit(0);\n" +
            "    static {\n" +
            "        System.out.println(/*EL:93*/\"clinit1\");\n" +
            "    }\n" +
            "    \n" +
            "    public RewriteInit(final int i) {\n" +
            "        System.out.println(/*EL:97*/i);\n" +
            "    }\n" +
            "    \n" +
            "    /*SL:100*/private RewriteInit middle = new RewriteInit(false);\n" +
            "    \n" +
            "    public RewriteInit(final boolean flag) {\n" +
            "        System.out.println(/*EL:103*/flag);\n" +
            "        /*SL:104*/this.test = new RewriteInit(0);\n" +
            "    }\n" +
            "    \n" +
            "    private RewriteInit test;\n" +
            "    static {\n" +
            "        System.out.println(/*EL:108*/\"clinit2\");\n" +
            "    }\n" +
            "    /*SL:111*/private RewriteInit bottom = new RewriteInit(true);\n" +
            "    static {\n" +
            "        System.out.println(/*EL:114*/\"clinit3\");\n" +
            "    }\n" +
            "}"
        );
    }
    
    @Test
    public void testRewriteInit2() throws Throwable {
        verifyOutput(
                RewriteInit2.class,
                lineNumberSettings(),
                "static class RewriteInit2 {\n" +
                        "    /*SL:120*/private RewriteInit2 top = new RewriteInit2(0);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:124*/\"clinit1\");\n" +
                        "    }\n" +
                        "    \n" +
                        "    public RewriteInit2(final int i) {\n" +
                        "        /*SL:128*/this(false);\n" +
                        "        System.out.println(/*EL:129*/i);\n" +
                        "    }\n" +
                        "    \n" +
                        "    /*SL:132*/private RewriteInit2 middle = new RewriteInit2(false);\n" +
                        "    \n" +
                        "    public RewriteInit2(final boolean flag) {\n" +
                        "        System.out.println(/*EL:135*/flag);\n" +
                        "    }\n" +
                        "    /*SL:136*/private RewriteInit2 test = new RewriteInit2(0);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:140*/\"clinit2\");\n" +
                        "    }\n" +
                        "    /*SL:143*/private RewriteInit2 bottom = new RewriteInit2(true);\n" +
                        "    static {\n" +
                        "        System.out.println(/*EL:146*/\"clinit3\");\n" +
                        "    }\n" +
                        "   class Inner {\n" +
                        "       Inner() {\n" +
                        "           System.out.println(/*EL:151*/\"Inner\");\n" +
                        "       }\n" +
                        "   }\n" +
                        "}"
                );
    }
    
    private static DecompilerSettings lineNumberSettings() {
        DecompilerSettings lineNumberSettings = defaultSettings();
        lineNumberSettings.setShowDebugLineNumbers(true);
        return lineNumberSettings;
    }
}
