package pro.jk.ejoker.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import pro.jk.ejoker.common.context.ContextRuntimeException;
import pro.jk.ejoker.common.system.enhance.StringUtilx;

/**
 * 提供类名扫描的工具
 * @author JiefzzLon
 *
 */
public class ClassNamesScanner {
	
	public static List<Class<?>> scanClass(String packageName) throws ClassNotFoundException {
		List<String> classNames = scan(packageName);
		List<Class<?>> result = new LinkedList<Class<?>>();
		for(String item : classNames)
			result.add(Class.forName(item));
		return result;
	}
	
	/**
	 * 扫描出指定包下的所有类的名字
	 * 不包含匿名类、内部类
	 * @param packageName
	 * @return List<String> // a list contains class with full package path.
	 */
	public static List<String> scan(String packageName){
		String packageDirName = packageName.replace('.', '/');
		List<String> classes = new ArrayList<String>();
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			while (dirs.hasMoreElements()){
				URL url = dirs.nextElement();
				String protocol = url.getProtocol(); //如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					//获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					//以文件的方式扫描整个包下的文件 并添加到集合中
					classes.addAll(scanPackagesClass(packageName, filePath));
				}else if ("jar".equals(protocol)){
					// 在jar中，package的信息就包含在文件目录结构上，
					// jar的根目录就是package的顶级路径 
					// 即 (jar下的路径) com/jiefzz/ejoker == (包名) com.jiefzz.ejoker
					// 但是文件目录结构包含比package更对信息。
					// 因此，要筛选出目标内容。
					// jar中文件夹会以 / 作结尾。
					JarFile jar;
					try {
						// 读取jar内容
						jar = ((JarURLConnection) url.openConnection()).getJarFile();
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// 如果是以/开头的，去除/ 正常情况下不会有这样的情况
							if (name.charAt(0) == '/') name = name.substring(1);
							// 非目标包下的文件，跳过
							if (!name.startsWith(packageDirName)) continue;
							// 排除非目标包但是其包路径的前部与目标相同的情况
							if (name.charAt(packageDirName.length()) != '/') continue;
							// 仅仅关注.class文件
							if (!name.endsWith(".class")) continue;
							// 排除匿名类
							if (name.contains("$")) continue;

							classes.add(name.substring(0, name.length()-6).replace('/', '.'));
						}
					} catch (IOException e) {
						throw new ContextRuntimeException(StringUtilx.fmt("Load class faild!!! [resource: {}]", url), e);
					}
				}
			}
		} catch (IOException e) {
			throw new ContextRuntimeException(StringUtilx.fmt("Load package faild!!! [package: {}]", packageDirName), e);
		}

		return classes;
	}

	private static List<String> scanPackagesClass(String packageName, String packagePath){
		File dir = new File(packagePath);
		if (!dir.exists() || !dir.isDirectory()) return null;
		List<String> classes = new ArrayList<String>();
		//取出class文件或者目录
		File[] dirfiles = dir.listFiles(new FileFilter() {
			/**
			 * 自定义过滤规则 子目录或则是以.class结尾的文件(编译好的java类文件)
			 */
			public boolean accept(File file) {
				return file.isDirectory() || (file.getName().endsWith(".class"));
			}
		});
		File file;
		for (int i = 0; i<dirfiles.length; i++) {
			file = dirfiles[i];
			String fileName = file.getName();
			if (!file.isDirectory()) {
				// 跳过匿名类
				if (fileName.contains("$")) continue;
				// 去掉 .class 这6个结尾的字符
				classes.add(packageName+"."+fileName.substring(0, fileName.length() - 6));
			} else {
				classes.addAll(scanPackagesClass(packageName + "." + file.getName(),file.getAbsolutePath()));
			}
		}
		return classes;
	}
}