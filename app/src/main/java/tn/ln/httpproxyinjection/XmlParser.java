package tn.ln.httpproxyinjection;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * xml文件解析工具类,用于解析host配置文件host_config.xml
 * Created by yuanxx on 2017/9/28.
 */

public class XmlParser {
    private static final String TAG = "XmlParser";
    private Context mContext;
    private static XmlParser instance;
    private Resources mResources;
    private XmlResourceParser mXmlParser;

    public XmlParser(Context context) {
        mContext = context;
        mResources = mContext.getResources();
        mXmlParser = mResources.getXml(R.xml.host_config);  //获取xml源文件
    }

    public static XmlParser getInstance(Context context) {
        if (instance == null) {
            instance = new XmlParser(context);
        }
        return instance;
    }

    /**
     * 读取xml文件
     */
    public void readXmlFile() {
        //1.得到Resources资源
        Resources r = mContext.getResources();
        //通过Resources，获得XmlResourceParser实例  
        XmlResourceParser xmlParser = r.getXml(R.xml.host_config);  //获取xml源文件

        //2.在获取节点属性前，加一个判断，判断开始和结束
        //如果没有到文件尾继续执行
        int counter = 0;
        StringBuilder sb = new StringBuilder("");
        try {
            while (xmlParser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                //如果是开始标签  
                if (xmlParser.getEventType() == XmlResourceParser.START_TAG) {
                    //获取标签名称  
                    String name = xmlParser.getName();
                    //判断标签名称是否等于config
                    if (name.equals("config")) {
                        counter++;
                        //获得标签属性追加到StringBuilder中  
                        sb.append("=======================\n第" + counter + "条信息：" + "\n");
                        sb.append("flag:" + xmlParser.getAttributeValue(0) + "\n");
                        sb.append("host:" + xmlParser.getAttributeValue(1) + "\n");
                        sb.append("ip:" + xmlParser.getAttributeValue(2) + "\n");
                        sb.append("type:" + xmlParser.getAttributeValue(3) + "\n");
                    }
                } else if (xmlParser.getEventType() == XmlPullParser.END_TAG) {
                } else if (xmlParser.getEventType() == XmlPullParser.TEXT) {
                }
                //下一个标签  
                xmlParser.next();
            }
            Log.i(TAG, "readXmlFile: \n" + sb.toString());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getIP(String flag, String hostName) {
        String ip = "";
        try {
            while (mXmlParser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if (mXmlParser.getEventType() == XmlResourceParser.START_TAG) {
                    String name = mXmlParser.getName();
                    if (name.equals("config")) {
                        if (flag.equals(mXmlParser.getAttributeValue(0)) && hostName.equals(mXmlParser.getAttributeValue(1))) {
                            return mXmlParser.getAttributeValue(2);
                        }
                    }
                } else if (mXmlParser.getEventType() == XmlPullParser.END_TAG) {
                } else if (mXmlParser.getEventType() == XmlPullParser.TEXT) {
                }
                //下一个标签
                mXmlParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ip;
    }
}
