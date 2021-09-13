package cn.wx.proxy.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "proxy.socket")
public interface SocketProxyConfig {

  @WithDefault("1080")
  Integer port();
}
