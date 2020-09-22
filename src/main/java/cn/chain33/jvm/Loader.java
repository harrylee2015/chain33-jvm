package cn.chain33.jvm;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 自定义的类加载器【子类优先】
 */
public class Loader extends ClassLoader {
    /**
     * lib:表示加载的文件在jar包中
     * 类似tomcat就是{PROJECT}/jvm/lib/
     */
    private String lib;
    /**
     * classes:表示加载的文件是单纯的class文件
     * 类似tomcat就是{PROJECT}/jvm/classes/
     */
    private String classes;
    /**
     * 采取将所有的jar包中的class读取到内存中
     * 然后如果需要读取的时候，再从data中查找
     * 缓存
     */
    private static HashMap<String, HashMap<String, byte[]>> data = new HashMap<String, HashMap<String, byte[]>>();
    /**
     * contractClass 类文件->合约名称
     */
    private static HashMap<String, String> contractClass = new HashMap<String, String>();
    /**
     * entryClass 合约名称->合约入口类
     */
    private static HashMap<String, String> entryClass = new HashMap<String, String>();

    /**
     * 只需要指定项目路径就好
     * 默认jar加载路径是目录下{PROJECT}/jvm/lib/
     * 默认class加载路径是目录下{PROJECT}/jvm/classes/
     *
     * @throws MalformedURLException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public Loader() throws NoSuchMethodException, SecurityException, MalformedURLException {
    }

    /**
     * 合约是否存在
     *
     * @param contractName
     * @return
     */
    public static boolean isExist(String contractName) {
        if (data.containsKey(contractName)) {
            return true;
        }
        return false;
    }

    /**
     * 获取合约执行类
     *
     * @param contractName
     * @return
     */
    public static String getEntryClass(String contractName) {
        return entryClass.get(contractName);
    }

    /**
     * 加载jar包
     *
     * @param contractName jar包名称,不带.jar
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws MalformedURLException
     */
    public Loader(String contractName) throws NoSuchMethodException, SecurityException, MalformedURLException {
        File directory = new File(contractName + ".jar");
        String path = directory.getAbsolutePath();
        if (data.containsKey(contractName)) {
            System.out.println("xxxxxx");
            return;
        }
        preReadJarFile(path);

    }

    /**
     * 按照父类的机制，如果在父类中没有找到的类
     * 才会调用这个findClass来加载
     * 这样只会加载放在自己目录下的文件
     * 而系统自带需要的class并不是由这个加载
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
     * 从map中查到已经加载的类
     *
     * @param name 包名和 tx,query所在的静态方法类同名
     * @return
     */
    private byte[] getClassFromMap(String name) {
        //获取合约名
        System.out.println("contractClass.get(name):" + name);
        if (contractClass.containsKey(name)) {
            //TODO 去除map中的引用，避免GC无法回收
            System.out.println("contract name:" + contractClass.get(name));
            System.out.println("contractClass.get(name):" + data.get(contractClass.get(name)));
            return data.get(contractClass.get(name)).get(name);
        }
        return null;
    }

    /**
     * 从指定的classes文件夹下找到文件
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
     * 预读lib下面的包
     */
    private void preReadJarFile() {

        List<File> list = scanDir();
        for (File f : list) {
            JarFile jar;
            try {
                jar = new JarFile(f);
                readJAR(jar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 预读指定的jar包
     */
    private void preReadJarFile(String f) {
        try {
            JarFile jar = new JarFile(f);
            readJAR(jar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取一个jar包内的class文件，并存在当前加载器的map中
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
            System.out.println("name:" + name);
            if (name.endsWith(".class")) {
                String clss = name.replace(".class", "").replaceAll("/", ".");
                File file = new File(jar.getName().trim());
                contractName = file.getName().replace(".jar", "");
                //判断类文件是否为入口类
                if (clss.endsWith(contractName)) {
                    entryClass.put(contractName, clss);
                }
                //类文件映射合约名称
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
                map.put(clss, cc);//保存下来
            }
        }
        if (map.isEmpty()) {
            return;
        }
        //把已经加载的jar保存到data中
        this.data.put(contractName, map);
    }


    /**
     * 扫描lib下面的所有jar包
     *
     * @return
     */
    private List<File> scanDir() {
        List<File> list = new ArrayList<File>();
        File[] files = new File(lib).listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".jar"))
                list.add(f);
        }
        return list;
    }

    /**
     * 添加一个jar包到加载器中去。
     *
     * @param jarPath
     * @throws IOException
     */
    public void addJar(String jarPath) throws IOException {
        File file = new File(jarPath);
        if (file.exists()) {
            JarFile jar = new JarFile(file);
            readJAR(jar);
        }
    }

    // 定义一个主方法
    public static void main(String[] args) {
        double upTime;//方法的执行时间(秒)
        long startTime = System.currentTimeMillis(); //获取开始时间
        // 如果运行该程序时没有参数，即没有目标类
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        // 第一个参数是需要运行的类
        String progClass = args[0];
        // 剩下的参数将作为运行目标类时的参数，
        // 将这些参数复制到一个新数组中
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        try {
            Loader loader = new Loader(progClass);
            System.out.println("processClass:" + getEntryClass(progClass));
            // 加载需要运行的类
            Class<?> clazz = loader.loadClass(getEntryClass(progClass));
            System.out.println("processClass:" + getEntryClass(progClass));
            // 获取需要运行的类的主方法
            Method main = clazz.getMethod("main", (new String[0]).getClass());
            Object[] argsArray = {progArgs};
            main.invoke(null, argsArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis(); //获取结束时间
        upTime = new BigDecimal(endTime - startTime).divide(new BigDecimal(1000)).doubleValue();//耗时(秒)
        System.out.println("exc cost time:" + upTime);
    }


    public static void tx(String[] args) throws Exception {
        // 如果运行该程序时没有参数，即没有目标类
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        // 第一个参数是需要运行合约名称
        String contractName = args[0];
        // 剩下的参数将作为运行目标类时的参数，
        // 将这些参数复制到一个新数组中
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        Loader loader = new Loader();
        //检测合约是否存在
        if (!isExist(contractName)) {
            loader = new Loader(contractName);
        }
        // 加载需要运行的类
        Class<?> clazz = loader.loadClass(getEntryClass(contractName));
        // 获取需要运行的类的主方法
        Method tx = clazz.getMethod("tx", (new String[0]).getClass());
        Object[] argsArray = {progArgs};
        tx.invoke(null, argsArray);
    }

    public static String[] query(String[] args) throws Exception {
        // 如果运行该程序时没有参数，即没有目标类
        if (args.length < 1) {
            System.out.println("java CompileClassLoader ClassName");
        }
        // 第一个参数是需要运行合约名称
        String contractName = args[0];
        // 剩下的参数将作为运行目标类时的参数，
        // 将这些参数复制到一个新数组中
        String[] progArgs = new String[args.length - 1];
        System.arraycopy(args, 1, progArgs
                , 0, progArgs.length);
        Loader loader = new Loader();
        //检测合约是否存在
        if (!isExist(contractName)) {
            loader = new Loader(contractName);
        }
        // 加载需要运行的类
        Class<?> clazz = loader.loadClass(getEntryClass(contractName));
        // 获取需要运行的类的主方法
        Method query = clazz.getMethod("query", (new String[0]).getClass());
        Object[] argsArray = {progArgs};
        Object result = query.invoke(null, argsArray);
        return (String[]) result;
    }
}
