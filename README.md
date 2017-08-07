# README
---

Add document comments to Ruby functions in Intellij or RubyMine

see https://www.jetbrains.com/help/ruby/documenting-source-code-in-rubymine.html for more info.

---


Move cursor to the element, and press the shortcut to add the Ruby doc comments.

Shortcuts:

-  `ctrl + shift + P` in Windows and Linux
-  `⌘  + ⇧ + P` (command shift P) in Mac os


---


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
# @param [Object]  num
# @param [Object]  several_variants
# @param [Object]  new
# @return [String]
def encode!(test, num = 1, *several_variants, **new)
  ''
end

```