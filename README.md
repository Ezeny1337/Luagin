<p align="center">
  <img src="https://github.com/user-attachments/assets/a2bc8355-3bec-4bf2-ad99-7bc78924fcc4" alt="Luagin Logo" height="200" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Kotlin-orange?logo=kotlin" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue" />
  <img src="https://img.shields.io/github/v/release/Ezeny1337/Luagin?label=Release&color=green" />
</p>

<p align="center">
  📑 <a href="#-overview">Overview</a> • <a href="#-documents">Documents</a> • <a href="#-compatibility">Compatibility</a> • <a href="#%EF%B8%8F-support">Support</a> • <a href="#%EF%B8%8F-license">License</a>
</p>

<p align="center">
  🌐 <a href="./README.md">English</a> | <a href="./README_zh.md">中文</a>
</p>

---

## 📖 Overview

**Luagin** is a plugin based on the **bukkit API**. It allows developers to highly customize the server through Lua scripts in an extremely effective and efficient manner.

### 🚀 Advantages

| Features                             | Advantages                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Kotlin Implementation                | More concise, modern and secure syntax features.                                                            |
| LuaJIT Implementation                | Extremely high running speed, complete support for Lua features, and support for bit and FFI libraries.     |
| Friendly API design                  | High encapsulation, fluent chainable calls, excellent readability. More convenient Java object interaction. |
| Friendly API docs                    | Easy to read, professional API docs.                                                                        |
| Inter-script communication           | Be competent for large and complex plugin development, even more modular.                                   |
| Koin DI framework and modular design | High cohesion and low coupling. High readability and maintainability.                                       |
| Translation                          | Language configs in different countries or regions that can be contributed by the community.                |

### ❓ Why Lua

1. **Easy to learn**: Lua is a **lightweight, simple yet powerful** scripting language. Even beginners can quickly get started with it.
2. **Efficient**: With Luagin, you **don't need** to build and manage **complex projects**, write **extensive boilerplate code**, or navigate through **hard-to-read APIs**, like you would when developing Java plugins.
3. **Hot update**: **Reloading** scripts takes effect immediately, greatly improving the development experience and reducing the cost of trial and error.
4. **Security**: Lua scripts run in a **controlled environment**, making it highly unlikely for them to crash the server or cause other serious issues.
5. **FFI Library**: Through LuaJIT's FFI library, you can directly interact with C, and **perform underlying system operations**.

## 📚 Documents

https://ezeny.gitbook.io/luagin-docs

    Due to insufficient time, the document currently only contains necessary
    information, which means you need to search for relevant information about
    some classes and their methods in Bukkit's documentation.

## 🧪 Compatibility

- The supported game versions depend on the **server-side API**.
- The support for `Bukkit` and its branches depends on the compatibility between the `Spigot` API and them. (**currently almost compatible**)

## 🛠️ Support

If you want to report bugs or suggest new features, you can [open an issue on Github](https://github.com/Ezeny1337/Luagin/issues).

If you need help using Luagin, please contact me on Discord: **ez3nyck**

## ⚖️ License
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
