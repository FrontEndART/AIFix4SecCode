import javalang
from smart_open import open as sopen
import traceback

# with open("HelloWorld.java", "rt") as file:
#     tree = javalang.parse.parse(file.read())


def tokenize(source):
    tokens = list(javalang.tokenizer.tokenize(source))
    tokenized = []

    tokens_of_interest = {"Keyword", "Operator", "Separator", "Modifier", "BasicType"}

    for token in tokens:
        token_name = token.__class__.__name__
        token_val = token.__dict__["value"]
        if token_name in tokens_of_interest:
            tokenized.append(token_name + "_" + token_val)
        else:
            tokenized.append(token_name)

    return tokenized


def tokens_from_token_strings(token_strings):
    javatoken_classes = dict_from_subclasses(javalang.tokenizer.JavaToken)

    result = []
    for token_str in token_strings:
        parts = token_str.split("_")
        token_name = parts[0]
        value = parts[1] if len(parts) == 2 else ""
        if token_name == "Annotation":
            value = "@"
        try:
            result.append(javatoken_classes[token_name](value))
        except KeyError as _:
            pass
    return result


def dict_from_subclasses(cls):
    subclasses = dict()
    for subcls in cls.__subclasses__():
        subclasses[subcls.__name__] = subcls
        subclasses.update(dict_from_subclasses(subcls))
    # subclasses = {subcls.__name__: subcls for subcls in cls.__subclasses__()}
    # subclasses[cls.__name__] = cls
    return subclasses


class LineSentenceFromTarGz:
    def __init__(self, source):
        self.source = source

    def __iter__(self):
        with sopen(self.source, "rb") as fin:
            for line in fin:
                try:
                    line = str(line, encoding="utf-8", errors="strict")
                    line = line[line.find("Keyword") :]
                    line = line.rstrip("\x00 \r\n").split(" ")
                    yield line
                except Exception as _:
                    pass


def save_nodes(source):
    corpus_iterable = LineSentenceFromTarGz(source)
    lines = []
    count = 1
    for line in corpus_iterable:
        if count <= 2100000:
            count += 1
            continue
        if count % 10000 == 0:
            with open(
                f"../java_projects_ast_nodes_blocks/{int(count / 10000)}.txt", "wt"
            ) as file:
                file.writelines(lines)
            print(count)
            lines = []
        try:
            parser = javalang.parser.Parser(tokens_from_token_strings(line))
            tree = parser.parse_compilation_unit()
            lines.append(
                " ".join([node.__class__.__name__ for path, node in tree]) + "\n"
            )
        except javalang.parser.JavaSyntaxError as jse:
            print(count)
            print(traceback.print_exception(jse))
        except Exception as e:
            print(count)
            print(traceback.print_exception(e))
        count += 1


def tree_from_token_strings(token_strings):
    parser = javalang.parser.Parser(tokens_from_token_strings(token_strings.split()))
    tree = None
    try:
        tree = parser.parse_compilation_unit()
    except javalang.parser.JavaSyntaxError as _:
        tree = parser.parse_block()
    return tree


# tokens = javalang.tokenizer.tokenize(source)
# parser = javalang.parse.Parser(tokens)
# tree = parser.parse_compilation_unit()
# l1 = [node.__class__.__name__ for path, node in tree]
# print(l1)

# tokens = tokens_from_token_strings(tokenize(source))
# parser = javalang.parse.Parser(tokens)
# tree = parser.parse_compilation_unit()
# l2 = [node.__class__.__name__ for path, node in tree]
# print(l2)


# tokens = tokenize(
#     """Modifier_public Keyword_class Identifier Separator_{ Modifier_private Modifier_static Identifier Identifier Operator_= String Separator_; Annotation Identifier Modifier_public Keyword_void Identifier Separator_( Separator_) Keyword_throws Identifier Separator_{ Identifier Identifier Operator_= Identifier Separator_. Identifier Separator_( Identifier Separator_) Separator_; BasicType_double Identifier Operator_= Identifier Separator_( Identifier Separator_) Separator_; Identifier Separator_( DecimalInteger Separator_, Identifier Separator_, DecimalInteger Separator_) Separator_; Separator_} Annotation Identifier Modifier_public Keyword_void Identifier Separator_( Separator_) Keyword_throws Identifier Separator_{ Identifier Identifier Operator_= Identifier Separator_. Identifier Separator_( Identifier Separator_) Separator_; Identifier Separator_( Identifier Separator_) Separator_; BasicType_double Identifier Operator_= Identifier Separator_( Identifier Separator_) Separator_; Identifier Separator_( DecimalFloatingPoint Separator_, Identifier Separator_, DecimalInteger Separator_) Separator_; Separator_} Separator_}"""
# )

# parser = javalang.parser.Parser(tokens_from_token_strings(tokens))
# tree = parser.parse_compilation_unit()
# print([node.__class__.__name__ for path, node in tree])

# print(javatoken_classes)
# print(make_tokens_from_token_strings(tokens))

# tokens = javalang.tokenizer.tokenize(
#     """class Simple{
#     public static void main(String args[]){
#         System.out.println("Hello Java");
#     }
# }  """
# )

# []

# print([token for token in tokens])
# parser = javalang.parser.Parser(tokens)
# tree = parser.parse_compilation_unit()

# print(tree.__class__)

# for path, node in tree:
#     # print(node.__class__.__name__)
#     # for n in path:
#     #     if n.__class__.__name__=="list":
#     #         for elem in n:
#     #             print(elem.__class__.__name__)
#     try:
#         pprint.pprint(path, indent=4)
#     except Exception as e:
#         pass

# print([(elem.__class__.__name__, elem.name) for elem in tree.types])
# print([(elem.__class__.__name__, elem.name) for elem in tree.types[0].body])

# print([node for node in tree.children if isinstance(node, (javalang.ast.Node))])
# print(tree.children)

# for path, node in tree:
#     print(node.__class__.__name__)
