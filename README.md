# Android---Video-Playback-and-Live-Broadcast-Content-Identifier

拆成两步：

1. ASR（Automatic Speech Recognition）语音转文字

把主播在直播切片中的讲话识别成文本。

model: Whisper 系列（OpenAI）,Whisper 是由 [OpenAI](https://zhida.zhihu.com/search?content_id=261647912&content_type=Article&match_order=1&q=OpenAI&zhida_source=entity) 开发的自动语音识别（ASR）系统，于 2022 年 9 月发布。它是一个基于 [Transformer](https://zhida.zhihu.com/search?content_id=261647912&content_type=Article&match_order=1&q=Transformer&zhida_source=entity) 架构的深度学习模型，能够将语音转换为文本，支持多种语言的语音识别、语音翻译和语言识别任务。

模型优势：

- **多语言支持**：支持 99 种语言的语音识别
- **多任务处理**：语音识别、语音翻译、语言检测
- **鲁棒性强**：对噪音、口音、语速变化有很好的适应性
- **标点符号**：自动添加标点符号和大小写
- **零样本学习**：无需针对特定领域进行微调

2. Dirty Words Detection（脏话识别）

对识别出来的文本做脏话分类 + 统计。

构建一个你自己的脏话表or 文本分类模型

**uggingFace 上现成的脏话分类模型（可直接用）**

### 英文方向：

- `unitary/unbiased-toxic-roberta`
- `HateXplain`
- `s-nlp/roberta-toxic`

### 中文方向：

- `hikari-ai/chinese-multi-dialect-toxic-model`
- `IDEA-CCNL/ErlangShen-Roberta-110M`