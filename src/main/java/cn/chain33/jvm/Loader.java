package cn.chain33.jvm;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
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
     * data  (合约名称->(类文名-->类字节码))
     * 用hashTable存储，线程安全，查询速度相对较快
     */
    private static Hashtable<String, Hashtable<String, byte[]>> data = new Hashtable<String, Hashtable<String, byte[]>>();
    /**
     * contractClass 类文件->合约名称
     */
    private static Hashtable<String, String> contractClass = new Hashtable<String, String>();
    /**
     * entryClass 合约名称->合约入口类
     */
    private static Hashtable<String, String> entryClass = new Hashtable<String, String>();

    /**
     *空的构造函数
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
     * 加载合约
     * @param contractName
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws MalformedURLException
     */
    public static void loadContract(String contractName) throws NoSuchMethodException, SecurityException, MalformedURLException {
        File directory = new File(contractName + ".jar");
        String path = directory.getAbsolutePath();
        if (data.containsKey(contractName)) {
            return;
        }
        Loader loader = new Loader();
        loader.preReadJarFile(path);

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
        if (contractClass.containsKey(name)) {
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
        Hashtable<String, byte[]> map = new Hashtable<String, byte[]>(64);
        Enumeration<JarEntry> en = jar.entries();
        String contractName = "";
        while (en.hasMoreElements()) {
            JarEntry je = en.nextElement();
            String name = je.getName();
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
            Loader loader = new Loader();
            //检测合约是否存在
            if (!isExist(progClass)) {
                //加载合约
                loader.loadContract(progClass);
            }
            System.out.println("processClass:" + getEntryClass(progClass));
            long startTime = System.nanoTime(); //获取开始时间
            // 加载需要运行的类
            Class<?> clazz = loader.loadClass(getEntryClass(progClass));
            long endTime = System.nanoTime(); //获取结束时间
            // 1s=10^9ns
            System.out.println("start time:"+startTime+"ns,end time:"+endTime+"ns,exc cost time:" + (endTime - startTime)+"ns");
            System.out.println("processClass:" + getEntryClass(progClass));
            // 获取需要运行的类的主方法
            Method main = clazz.getMethod("main", (new String[0]).getClass());
            Object[] argsArray = {progArgs};
            main.invoke(null, argsArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            loader.loadContract(contractName);
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
            loader.loadContract(contractName);
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