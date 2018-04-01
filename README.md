# README
---

[![Build Status](https://travis-ci.org/aristotll/RubyDocAdder.svg?branch=master)](https://travis-ci.org/aristotll/RubyDocAdder)

Add document comments to Ruby functions in Intellij or RubyMine.

see https://www.jetbrains.com/help/ruby/documenting-source-code-in-rubymine.html for more info.

---


Move cursor to the function name, and press the shortcut to add the Ruby doc comments.

Shortcuts:

-  `ctrl + shift + P` in Windows and Linux
-  `⌘  + ⇧ + P` (command shift P) in Mac os


---

![example]( https://raw.githubusercontent.com/aristotll/RubyDocAdder/master/rdoc-adder.gif )

---

Before
```ruby
def encode!(test, num = 1, *several_variants, **new)
  ''
end
```

After
```
# @param [Object]  test
# @param [Fixnum]  num
# @param [Array]  several_variants
# @param [Hash]  new
# @return [String]
def encode!(test, num = 1, *several_variants, **new)
  ''
end

```