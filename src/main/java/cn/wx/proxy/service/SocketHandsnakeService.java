package cn.wx.proxy.service;

import cn.wx.proxy.contant.SocketParams;
import cn.wx.proxy.contant.SocketProxyStage;
import cn.wx.proxy.util.SocketParseUtils;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.net.NetSocket;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class SocketHandsnakeService {


    /**
     * 处理握手
     *
     * @param context 存储值
     * @param socket  s
     * @param bytes   b
     */
    private void handlerHandshake(Context context, NetSocket socket, byte[] bytes) {
        // 解析socket版本
        SocketParams.VER version = SocketParseUtils.parseVer(bytes);
        if (version == null) {
            socket.close();
            return;
        }

        // 解析授权方式
        Set<SocketParams.METHOD> auths = SocketParseUtils.parseMethod(bytes);
        if (auths.contains(SocketParams.METHOD.None)) {
            socket.write(Buffer.buffer()
                    .appendByte(version.byteCode())
                    .appendByte(SocketParams.METHOD.None.byteCode()));
            context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.Cmd);
        } else if (auths.contains(SocketParams.METHOD.Passwd)) {
            socket.write(Buffer.buffer()
                    .appendByte(version.byteCode())
                    .appendByte(SocketParams.METHOD.Passwd.byteCode()));
            context.put(socket.writeHandlerID() + ":stage", SocketProxyStage.MethodAuth);
        } else {
            // 不支持其他登录方式
            socket.end(Buffer.buffer()
                    .appendByte(version.byteCode())
                    .appendByte(SocketParams.METHOD.NoAcceptable.byteCode()));
        }
    }
}
