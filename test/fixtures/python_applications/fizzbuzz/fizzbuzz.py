"""
FizzBuzz
https://medium.freecodecamp.org/a-software-engineering-survival-guide-fe3eafb47166
https://medium.freecodecamp.org/coding-interviews-for-dummies-5e048933b82b
This solution uses the following syntax features:
Modulo (%, remainder)
Strict equality (==)
Addition assignment (+=)
"""

import fibonacci
import palindrome as p
from foobar import foo, g
from bar import *
from foo import bb as foo_bb, cc as foo_cc


def fizzbuzz_print() -> None:
    """Print 1-100
    ---
    - Multiples of 3: Fizz
    - Multiples of 5: Buzz
    - Multiples of 3 and 5: FizzBuzz
    """
    for i in range(1, 101):
        out = ""
        if i % 3 == 0:
            out += "Fizz"
        if i % 5 == 0:
            out += "Buzz"
        print(out or i)


def fizzbuzz_list() -> list[int | str]:
    """Create a list 1-100
    ---
    - Multiples of 3 and 5: FizzBuzz
    - Multiples of 3: Fizz
    - Multiples of 5: Buzz
    - Else: integer
    """
    out: list[int | str] = []
    for i in range(100):
        if i % 3 == 0 and i % 5 == 0:
            out.insert(i, "FizzBuzz")
        elif i % 3 == 0:
            out.insert(i, "Fizz")
        elif i % 5 == 0:
            out.insert(i, "Buzz")
        else:
            out.insert(i, i)
    return out

def all():
    p("hello world")
    foo(10)
    b(5)
    foo_bb(55)


if __name__ == "__main__":
    print(fizzbuzz_list())
    print(fibonacci.f(7))
