# burp-comment-extractor
## 概述

提取HTTP响应数据包中的HTML/JS注释。

核心提取正则：

HTML注释：String htmlCommentRegex = "<!--(.*?)-->";

JS注释：String jsCommentRegex = "(?<!:)\\/\\/.*";

JS注释1：String jsCommentRegex1 = "\\/\\*(\\s|.)*?\\*\\/";



## 快速开始

插件开启后，会进行doPassiveScan，遇到注释后有两处输出点：

1）issue界面

![](https://github.com/theLSA/burp-comment-extractor/raw/master/demo/bce00.png)

2）自定义tab

![](https://github.com/theLSA/burp-comment-extractor/raw/master/demo/bce01.png)

![](https://github.com/theLSA/burp-comment-extractor/raw/master/demo/bce02.png)

![](https://github.com/theLSA/burp-comment-extractor/raw/master/demo/bce03.png)



## TODO

1.可能会增加敏感关键字匹配。

2.去重。



## 反馈

[issues](https://github.com/theLSA/burp-comment-extractor/issues)