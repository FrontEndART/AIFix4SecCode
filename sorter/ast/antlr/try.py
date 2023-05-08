from vectorize import BinaryASTVectorizer
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
import node.node as node
import numpy as np

source = """
public class A {
    public void setDate(Date date) {
        this.date = date;
    }
}"""

patched = """
public class A {
    public void setDate(Date date) {
        this.date = new Date(date.getTime());
    }
}"""

# source = """
# public class A {
#     public void setDate() {}
# }"""

# patched = """
# public class A {
#     public void setDate() {}
#     public void hello() {}
# }"""

vt = BinaryASTVectorizer()

# with open("java-grammar/examples/AllInOne7.java", "rt") as f:
#     sourceallinone7 = f.read()
# binAST = vt.createBinaryAST(sourceallinone7)
# binAST.print()

vectorized_original, vectorized_patched = vt.vectorize(source, patched)
# pca = PCA(0.95)
# print(pca.fit_transform([[10, 100, 1, 42], [12, 234, 10, 31]]))  # , [123, 1, 432, 2]]))
# vectors = pca.fit_transform(np.array([vectorized_original, vectorized_patched]))
print(vectorized_original)
