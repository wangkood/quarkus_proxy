package cn.wx.proxy.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "proxy.http")
public interface HttpProxyConfig {

  /**
   * d
   * @return 端口
   */
  @WithDefault("8090")
  Integer port();
}
