<p align="center">
  <img src="https://github.com/user-attachments/assets/a2bc8355-3bec-4bf2-ad99-7bc78924fcc4" alt="Luagin Logo" height="200" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Kotlin-orange?logo=kotlin" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue" />
  <img src="https://img.shields.io/github/v/release/Ezeny1337/Luagin?label=Release&color=green" />
</p>

<p align="center">
  📑 <a href="#-概述">概述</a> • <a href="#-文档">文档</a> • <a href="#%EF%B8%8F-配置">配置</a>• <a href="#-web-面板">Web 面板</a> • <a href="#-兼容性">兼容性</a> • <a href="#%EF%B8%8F-支持">支持</a> • <a href="#%EF%B8%8F-许可">许可</a>
</p>

<p align="center">
  🌐 <a href="./README.md">English</a> | <a href="./README_zh.md">中文</a>
</p>

---

## 📖 概述

**Luagin** 是一个基于 **Bukkit API** 与 **LuaJIT** 的插件，它使开发者能够通过 Lua 脚本以非常高效的方式对服务器进行高度定制

### 🚀 特性

| 特性               | 优势                                     |
|------------------|----------------------------------------|
| LuaJIT 实现        | 极高的运行速度，Lua 特性完整支持                     |
| 友好的 API 设计       | 高度抽象与封装、流畅的链式调用、优秀的可读性，更方便与 Java 对象的交互 |
| 友好的 API 文档       | 易于阅读的专业 API 文档                         |
| 脚本间通信            | 适用于大型复杂插件的开发，模块化程度更高                   |
| 多语言支持            | 支持不同国家或地区的语言配置，可由社区贡献                  |
| Web 面板           | 现代的 Web 面板可以帮助你直观方便地监控服务器数据和配置插件       |

### ❓ 为什么选择 Lua/Luagin

1. **易学易用**：Lua 是一门 **轻量级、简单而强大的** 脚本语言，即使是初学者也能迅速上手
2. **高效**：使用 Luagin，你 **无需** 像编写 Java 插件那样，构建和管理 **复杂的项目**，编写 **冗长的模板代码**，与 **难以阅读的
   API** 打交道
3. **热更新**：**重新加载** 脚本可以立即生效，大大提升开发体验并减少试错的成本
4. **安全性**：Lua 脚本运行在一个 **受控的环境** 中，不容易导致服务器崩溃或其他严重问题
5. **抽象与封装**：通过 Java 侧的 **高度抽象与封装** ，在 Lua 侧调用 API 就能极大**降低开发难度与时间**
6. **扩展**：**高度抽象与封装** 并不意味着开发者只能按照 Luagin 的意愿实现功能，相反，它可以调用**所有原服务端 API**，并且有
   **更多的扩展**

## 📚 文档

https://ezeny.gitbook.io/luagin-docs

    由于没有足够的时间，目前文档只包含必要的信息，这意味着你需要在 Bukkit 文档中查找
    原始的类及其方法的相关信息

## ⚙️ 配置

配置位于 - `plugins/Luagin/configs`

语言配置位于 - `plugins/Luagin/lang`

## 🌀 Web 面板

如果你在配置文件里启用了 web 面板, 可以通过 `localhost:9527` 访问它， 目前支持中文和英文

默认用户名和密码是"admin"

<img width="1919" height="939" alt="panel1" src="https://github.com/user-attachments/assets/07779e39-679b-4017-a985-7374866fa2db" />
<img width="1919" height="934" alt="panel2" src="https://github.com/user-attachments/assets/018addf4-d407-4e2a-ab2d-f1584ec05b7b" />

## 🧪 兼容性

- 支持的游戏版本取决于 **服务端 API**
- 对 `Bukkit` 及其分支的支持取决于 `Spigot` API 在它们之间的兼容性 (**目前几乎兼容**)

## 🛠️ 支持

如果你想报告 bug 或建议新功能，可以在 [GitHub 上开一个 issue](https://github.com/Ezeny1337/Luagin/issues)

如果你需要使用 Luagin 的帮助，可以通过 Discord 联系我：**ez3nyck**

## ⚖️ 许可

    Copyright [2025] [Ezeny1337]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
