<div align="center">
    <img width="192" src="app/src/main/res/drawable/icon_round.png" alt="logo"><br/>
    <img width="96" src="docs/images/logo.svg">

------
[![Releases](https://img.shields.io/github/downloads/MrXiaoM/Aoki/total?label=%E4%B8%8B%E8%BD%BD%E9%87%8F&logo=github)](https://github.com/MrXiaoM/Aoki/releases)
[![Stars](https://img.shields.io/github/stars/MrXiaoM/Aoki?label=%E6%A0%87%E6%98%9F&logo=github)](https://github.com/MrXiaoM/Aoki/stargazers)
![Android](https://img.shields.io/badge/%E5%AE%89%E5%8D%93-8-brightgreen?logo=android)

Aoki 是在 Android 平台上基于 [mirai](https://github.com/mamoe/mirai) 开发的登录处理器

本项目名称来源于

i-style project 推出的虚拟歌手[蒼姫ラピス (**Aoki** Lapis)](https://en.wikipedia.org/wiki/Aoki_Lapis)

项目图标由[人间工作](https://www.pixiv.net/artworks/103427447)绘制

</div>

## 注意事项

* 请确保你在**真实的** Android 设备上进行登录
* 请确保你已在该 Android 设备登录了**官方的** QQ 客户端
* Aoki 可以确保你在第一次登录时能够正常登录，不能保证已经触发过 235/237/45 的账号能登录
* 由于服务器策略更新，“迁移设备信息” 这种登录方法可能不再可用。

## 使用方法

先设法将你的本地 mirai 版本升级到 2.15.0-dev-98 或以上 (或 2.15.0-RC)。升级到开发版方法将会在以后放出。

使用插件将 ANDROID_PAD 协议的版本信息改为 8.8.88，如 [fix-protocol-version](https://github.com/cssxsh/fix-protocol-version) 插件。  
如果开启时提示“服务注册失败”，你可能还需要再安装 [KawaiiMiku](https://github.com/MrXiaoM/KawaiiMiku) 插件

在**真实的** Android 手机上安装 Aoki，

注意一定要在**真实的**手机上打开 Aoki，因为 Aoki 需要读取你的手机型号等信息生成 device.json。

打开并按照引导进行登录 **(使用平板协议)**。出现「登录成功」提示后，你可以

### 压缩并分享

登录成功后点击「打包并分享到…」，然后在弹出的分享面板中想办法将该文件发送到电脑上，比如 通过QQ文件传输助手发送到电脑，或者 使用蓝牙连接发送到电脑 等等。

你也可以在「账号管理」中找到「打包并分享到…」。

### 通过路径复制文件

使用任何你能想到的方法将手机储存目录下的 `Android/data/top.mrxiaom.mirai.aoki/files/AokiMirai` 文件夹传输到电脑上，可用的方法包括但不限于如下：

* 使用数据线复制文件夹
* 压缩文件夹，通过QQ文件传输助手发送到电脑

--------------

将从以上其中一种方法获得的 `bots` 文件夹中以qq号命名的文件夹，  
覆盖到 mirai 目录，然后你就可以在 mirai 中登录你的账户了。

**建议在导出文件后，等待半小时左右再登录，以免因为短时间内两次登录的 IP 归属地变动较大被风控。**

## 下载

在 [Github Releases](https://github.com/MrXiaoM/Aoki/releases) 下载最新版本

## 捐助

前往 [爱发电](https://afdian.net/a/mrxiaom) 捐助我。
