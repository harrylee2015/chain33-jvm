//package cn.chain33.jvm.dapp.guess;
package com.fuzamei.chain33;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Custom class loader [subclass first]
 */
public class Loader extends ClassLoader{

    private static final Loader INSTANCE = new Loader();

    public static final Loader getInstance() {
       return INSTANCE;
    }
    public Loader(){
        super();
    }
    /**
     * Read all the classes in the jar package into memory
     * Then if you need to read, look up from the cache data
     * data (contract name->(class file name-->class bytecode))
     */
    private static HashMap<String, HashMap<String, byte[]>> data = new HashMap<String, HashMap<String, byte[]>>();
    /**
     * contractClass  Class file -> contract name
     */
    private static HashMap<String, String> contractClass = new HashMap<String, String>();
    /**
     * entryClass   Contract Name -> Contract Entry Class
     */
    private static HashMap<String, String> entryClass = new HashMap<String, String>();

    /**
     * common   common class
     */
    private static HashMap<String, byte[]> common = new HashMap<String, byte[]>();
    private static String lib = "lib";
    private String classes;

    /**
     * Whether the contract exists
     *
     * @param contractName
     * @return
     */
    public static boolean isExist(String contractName) {
        if (INSTANCE.data.containsKey(contractName)) {
            return true;
        }
        return false;
    }

    /**
     * get contract entry class by contract name.
     *
     * @param contractName
     * @return
     */
    public static String getEntryClass(String contractName) {
        return INSTANCE.entryClass.get(contractName);
    }

    /**
     * load jar
     * @param op,pathName
     */
    public static int loadJar(int op,String pathName) {
        if (op==0){
            List<File> list = INSTANCE.scanDir(pathName);
            for (File f : list) {
                JarFile jar;
                try {
                    jar = new JarFile(f);
                    System.out.println(jar);
                    INSTANCE.readCommonJAR(jar);
                } catch (Exception e) {
                    e.printStackTrace();
                    return 1;
                }
            }
            return 0;
        }
        File directory = new File(pathName + ".jar");
        String path = directory.getAbsolutePath();
        if (INSTANCE.data.containsKey(pathName)) {
            return 0;
        }
        try {
            INSTANCE.preReadJarFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        String progClass = args[0];
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        try {
            Loader loader = getInstance();
            loadJar(0,"lib");
            System.out.println("loadLib jar:");
            long startTime = System.nanoTime(); //获取开始时间
            for(Map.Entry<String, byte[]> entry : INSTANCE.common.entrySet()){
                String mapKey = entry.getKey();
                byte[] mapValue = entry.getValue();
                System.out.println(mapKey+":"+mapValue.toString());
            }
            long endTime = System.nanoTime(); //获取结束时间
            // 1s=10^9ns
            System.out.println("start time:"+startTime+"ns,end time:"+endTime+"ns,exc cost time:" + (endTime - startTime)+"ns");
            if (!isExist(progClass)) {
                loadJar(1,progClass);
            }
            System.out.println("processClass:" + INSTANCE.getEntryClass(progClass));
//            long startTime = System.nanoTime();
            Class<?> clazz = loader.loadClass(loader.getEntryClass(progClass));
//            long endTime = System.nanoTime();
//            System.out.println("start time:" + startTime + "ns,end time:" + endTime + "ns,exc cost time:" + (endTime - startTime) + "ns");
            System.out.println("classLoader:" +clazz.getClassLoader().toString());
            System.out.println("processClass:" + getEntryClass(progClass));
            Method main = clazz.getMethod("main", (new String[0]).getClass());

            Object[] argsArray = {progArgs};
            main.invoke(null, argsArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int tx(String[] args) {
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        String contractName = args[0];
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        Loader loader = new Loader();
        if (!isExist(contractName)) {
            loader.loadJar(1,contractName);
        }
        try {
            Class<?> clazz = loader.loadClass(getEntryClass(contractName));
            Method tx = clazz.getMethod("tx", (new String[0]).getClass());
            Object[] argsArray = {progArgs};
            tx.invoke(null, argsArray);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return 2;
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return 3;
        } catch (IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            e.printStackTrace();
            return 4;
        }
        return 0;
    }

    public static String[] query(String[] args) {
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        String contractName = args[0];
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        Loader loader = new Loader();
        if (!isExist(contractName)) {
            loader.loadJar(1,contractName);
        }
        try {
            Class<?> clazz = loader.loadClass(getEntryClass(contractName));
            Method query = clazz.getMethod("query", (new String[0]).getClass());
            Object[] argsArray = {progArgs};
            Object result = query.invoke(null, argsArray);
            return (String[]) result;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            e.printStackTrace();

        }
        return null;
    }

    /**
     * According to the mechanism of the parent class, if the class is not found in the parent class
     * Will call this findClass to load
     * This will only load files placed in your own directory
     * The class required by the system is not loaded by this
     */
    @Override
    protected Class<?> findClass(String name) {
        System.out.println("findClass:" + name);
        try {
            byte[] result = getClassFromMap(name);
            if (result == null) {
                throw new FileNotFoundException();
            } else {
                return defineClass(name, result, 0, result.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Find the loaded class from the cache data.
     *
     * @param name
     * @return
     */
    private byte[] getClassFromMap(String name) {
        //load contract class
        if (INSTANCE.contractClass.containsKey(name)) {
            return INSTANCE.data.get(contractClass.get(name)).get(name);
        }
        //load common class
        if (INSTANCE.common.containsKey(name)) {
            return INSTANCE.common.get(name);
        }
        return null;
    }

    /**
     * Find the file from the specified classes folder.
     *
     * @param name
     * @return
     */
    private byte[] getClassFromFileOrMap(String name) {
        String classPath = classes + name.replace('.', File.separatorChar) + ".class";
        File file = new File(classPath);
        if (file.exists()) {
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                while ((bytesNumRead = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                return baos.toByteArray();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return null;
    }

    /**
     * Pre-read packages under lib
     */
    private void preReadLibJarFile(String path) throws IOException {

        List<File> list = scanDir(path);
        for (File f : list) {
            JarFile jar;
            jar = new JarFile(f);
            readCommonJAR(jar);
        }
    }

    /**
     * Pre-read the specified jar package
     *
     * @param f
     * @throws IOException
     */
    private void preReadJarFile(String f) throws IOException {
        JarFile jar = new JarFile(f);
        readJAR(jar);
    }

    private void readCommonJAR(JarFile jar) throws IOException {
        Enumeration<JarEntry> en = jar.entries();
        while (en.hasMoreElements()) {
            JarEntry je = en.nextElement();
            String name = je.getName();
            if (name.endsWith(".class")) {
                String clss = name.replace(".class", "").replaceAll("/", ".");
                if (this.findLoadedClass(clss) != null) continue;

                InputStream input = jar.getInputStream(je);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                while ((bytesNumRead = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                byte[] cc = baos.toByteArray();
                input.close();
                this.common.put(clss, cc);
            }
        }
    }

    /**
     * Read the class file in a jar package and store it in the map of the current loader
     *
     * @param jar
     * @throws IOException
     */
    private void readJAR(JarFile jar) throws IOException {
        HashMap<String, byte[]> map = new HashMap<String, byte[]>(64);
        Enumeration<JarEntry> en = jar.entries();
        String contractName = "";
        while (en.hasMoreElements()) {
            JarEntry je = en.nextElement();
            String name = je.getName();
            if (name.endsWith(".class")) {
                String clss = name.replace(".class", "").replaceAll("/", ".");
                File file = new File(jar.getName().trim());
                contractName = file.getName().replace(".jar", "");

                if (clss.endsWith(contractName)) {
                    entryClass.put(contractName, clss);
                }

                contractClass.put(clss, contractName);
                if (this.findLoadedClass(clss) != null) continue;

                InputStream input = jar.getInputStream(je);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesNumRead = 0;
                while ((bytesNumRead = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesNumRead);
                }
                byte[] cc = baos.toByteArray();
                input.close();
                map.put(clss, cc);
            }
        }
        if (map.isEmpty()) {
            return;
        }

        this.data.put(contractName, map);
    }

    /**
     * Scan all jar packages under lib
     *
     * @return
     */
    private List<File> scanDir(String path) {
        List<File> list = new ArrayList<File>();
        File[] files = new File(path).listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".jar"))
                list.add(f);
        }
        return list;
    }

    public void addJar(String jarPath) throws IOException {
        File file = new File(jarPath);
        if (file.exists()) {
            JarFile jar = new JarFile(file);
            readJAR(jar);
        }
    }
}
