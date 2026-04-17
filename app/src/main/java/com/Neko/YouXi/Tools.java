package com.Neko.YouXi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

public class Tools {
    private static final String FILE_NAME = "AppConfig.cfg";
        
    public static void 归递只读(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        归递只读(file);
                    } else if (file.getName().endsWith(".dex")) {
                        file.setReadOnly();
                        file.setReadable(true, false);
                        file.setWritable(false);
                    }
                }
            }
        }
    }
    
    public static boolean 归递删除(File folder) {
        if (folder == null || !folder.exists()) {
            return true; // 文件夹不存在，视为删除成功
        }
    
        if (folder.isDirectory()) {
            // 列出文件夹中的所有文件和子文件夹
            File[] files = folder.listFiles();
            if (files != null) {
                    for (File file : files) {
                    // 递归删除子文件或子文件夹
                    归递删除(file);
                }
            }
        }
    
        // 删除空文件夹或文件
        return folder.delete();
    }
    
    public static String 取自身版本号(Context c) {
        try {
        	PackageInfo info = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return info.versionName;
        } catch(NameNotFoundException err) {
        	return "1.0";
        }
    }

    public static void 申请ROOT() {
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/*检测手机是否有ROOT*/
	public static boolean 检测ROOT() {
		File file=null;
		String[]paths={"/system/bin/","/system/xbin/"};
		try {
			for (String path:paths) {
				file = new File(path + "su");
				if (file.exists() && file.canExecute()) {
					return true;
				} else {
					return false;
				}
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return false;
	}

	/*创建文件*/
	public static void 创建文件(String path) {
		File mFile=new File(path);
		if (mFile.exists()) {
			mFile.delete();
		}
		try {
			mFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*创建文件夹*/
	public static void 创建文件夹(String 路径) {
		File file=new File(路径);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	/*写入内容到文件*/
    public static void 写入文件(String 路径, String 内容) {
        try {
            FileWriter fw = new FileWriter(路径);
            fw.write(内容);
            fw.close();
        } catch (IOException e) {}
    }

    public static boolean 删除文件(String folder) {
        if (folder == null || folder.length() == 0 || folder.trim().length() == 0) {
            return true;
        }
        File file = new File(folder);
        if (!file.exists()) {
            return true;
        }
        if (file.isFile()) {
            return file.delete();
        }
        if (!file.isDirectory()) {
            return false;
        }
        for (File f : file.listFiles()) {
            if (f.isFile()) {
                f.delete();
            } else if (f.isDirectory()) {
                new File((f.getAbsolutePath())).delete();
            }
        }
        return file.delete();
    }

	/*读取文件里的内容*/
	public static String 读取文件(String 路径) {
		String str="";
		try {
			File urlFile=new File(路径);
			InputStreamReader isr=new InputStreamReader(new FileInputStream(urlFile), "UTF-8");
			BufferedReader br=new BufferedReader(isr);
			String mimeTypeLine=null;
			while ((mimeTypeLine = br.readLine()) != null) {
				str = str + mimeTypeLine;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}
    
    public static String 取Md5(String filePath) {
        try {
            InputStream fis = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            fis.close();
        
            byte[] md5Bytes = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : md5Bytes) {
                // 每个字节转换为2位十六进制，不足补0
                hexString.append(String.format("%02x", b & 0xff));
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void 申请悬浮窗权限(Context mContext) {
        if (mContext == null) {
            return;
        }

        if (!(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(mContext))) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mContext.getPackageName()));
            mContext.startActivity(intent);
        }
        return;
    }
    
    public static void 存储(Context context , String key, Object object) {

        String type = object.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if ("String".equals(type)) {
            editor.putString(key, (String)object);
        }
        else if ("Integer".equals(type)) {
            editor.putInt(key, (Integer)object);
        }
        else if ("Boolean".equals(type)) {
            editor.putBoolean(key, (Boolean)object);
        }
        else if ("Float".equals(type)) {
            editor.putFloat(key, (Float)object);
        }
        else if ("Long".equals(type)) {
            editor.putLong(key, (Long)object);
        }

        editor.commit();
    }

    public static Object 读取(Context context , String key, Object defaultObject){
        String type = defaultObject.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

        if("String".equals(type)){
            return sp.getString(key, (String)defaultObject);
        }
        else if("Integer".equals(type)){
            return sp.getInt(key, (Integer)defaultObject);
        }
        else if("Boolean".equals(type)){
            return sp.getBoolean(key, (Boolean)defaultObject);
        }
        else if("Float".equals(type)){
            return sp.getFloat(key, (Float)defaultObject);
        }
        else if("Long".equals(type)){
            return sp.getLong(key, (Long)defaultObject);
        }

        return null;
    }
	
	public static String UrlPost(String ur, String byteString) {
        String str="";		
        try {
            URL url=new URL(ur);
            HttpURLConnection HttpURLConnection=(HttpURLConnection) url.openConnection();
            HttpURLConnection.setReadTimeout(9000);
            HttpURLConnection.setRequestMethod("POST");
            OutputStream outputStream = HttpURLConnection.getOutputStream();
            outputStream.write(byteString.getBytes());
            BufferedReader BufferedReader=new BufferedReader(new InputStreamReader(HttpURLConnection.getInputStream()));
            String String="";
            StringBuffer StringBuffer=new StringBuffer();
            while ((String = BufferedReader.readLine()) != null) {
                StringBuffer.append(String);
            }
            str = StringBuffer.toString();
        } catch (IOException e) {}
        return str;
    }
	
	public static boolean isVpnUsed() {//vpn检测
        try {
            List<NetworkInterface> nis = Collections.list(NetworkInterface.getNetworkInterfaces());
            if (nis != null) {
                for (NetworkInterface intf : nis) {
                    if (!intf.isUp() || intf.getInterfaceAddresses().size() == 0) {
                        continue;
                    }
                    if ("tun0".equals(intf.getName()) || "ppp0".equals(intf.getName())) {
                        return true; // The VPN is up
                    }
                }
            }
        } catch (Throwable e) {
			e.printStackTrace();
        }
        return false;
    }
}
