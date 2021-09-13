package cn.wx.proxy;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * 一个代理服务器
 *
 * @author wangxin
 */
@QuarkusMain
public class ProxyApplication implements QuarkusApplication {

    @Override
    public int run(String... args) {return 10; }

    public static void main(String[] args) {
        Quarkus.run(ProxyApplication.class, args);
    }
}
