---
navigation_title: "Path hierarchy"
mapped_pages:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-pathhierarchy-tokenizer.html
---

# Path hierarchy tokenizer [analysis-pathhierarchy-tokenizer]


The `path_hierarchy` tokenizer takes a hierarchical value like a filesystem path, splits on the path separator, and emits a term for each component in the tree. The `path_hierarcy` tokenizer uses Lucene’s [PathHierarchyTokenizer](https://lucene.apache.org/core/10_0_0/analysis/common/org/apache/lucene/analysis/path/PathHierarchyTokenizer.md) underneath.


## Example output [_example_output_14]

```console
POST _analyze
{
  "tokenizer": "path_hierarchy",
  "text": "/one/two/three"
}
```

The above text would produce the following terms:

```text
[ /one, /one/two, /one/two/three ]
```


## Configuration [_configuration_15]

The `path_hierarchy` tokenizer accepts the following parameters:

`delimiter`
:   The character to use as the path separator. Defaults to `/`.

`replacement`
:   An optional replacement character to use for the delimiter. Defaults to the `delimiter`.

`buffer_size`
:   The number of characters read into the term buffer in a single pass. Defaults to `1024`. The term buffer will grow by this size until all the text has been consumed. It is advisable not to change this setting.

`reverse`
:   If `true`, uses Lucene’s [ReversePathHierarchyTokenizer](http://lucene.apache.org/core/10_0_0/analysis/common/org/apache/lucene/analysis/path/ReversePathHierarchyTokenizer.md), which is suitable for domain–like hierarchies. Defaults to `false`.

`skip`
:   The number of initial tokens to skip. Defaults to `0`.


## Example configuration [_example_configuration_9]

In this example, we configure the `path_hierarchy` tokenizer to split on `-` characters, and to replace them with `/`. The first two tokens are skipped:

```console
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "my_tokenizer"
        }
      },
      "tokenizer": {
        "my_tokenizer": {
          "type": "path_hierarchy",
          "delimiter": "-",
          "replacement": "/",
          "skip": 2
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "one-two-three-four-five"
}
```

The above example produces the following terms:

```text
[ /three, /three/four, /three/four/five ]
```

If we were to set `reverse` to `true`, it would produce the following:

```text
[ one/two/three/, two/three/, three/ ]
```


## Detailed examples [analysis-pathhierarchy-tokenizer-detailed-examples]

A common use-case for the `path_hierarchy` tokenizer is filtering results by file paths. If indexing a file path along with the data, the use of the `path_hierarchy` tokenizer to analyze the path allows filtering the results by different parts of the file path string.

This example configures an index to have two custom analyzers and applies those analyzers to multifields of the `file_path` text field that will store filenames. One of the two analyzers uses reverse tokenization. Some sample documents are then indexed to represent some file paths for photos inside photo folders of two different users.

```console
PUT file-path-test
{
  "settings": {
    "analysis": {
      "analyzer": {
        "custom_path_tree": {
          "tokenizer": "custom_hierarchy"
        },
        "custom_path_tree_reversed": {
          "tokenizer": "custom_hierarchy_reversed"
        }
      },
      "tokenizer": {
        "custom_hierarchy": {
          "type": "path_hierarchy",
          "delimiter": "/"
        },
        "custom_hierarchy_reversed": {
          "type": "path_hierarchy",
          "delimiter": "/",
          "reverse": "true"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "file_path": {
        "type": "text",
        "fields": {
          "tree": {
            "type": "text",
            "analyzer": "custom_path_tree"
          },
          "tree_reversed": {
            "type": "text",
            "analyzer": "custom_path_tree_reversed"
          }
        }
      }
    }
  }
}

POST file-path-test/_doc/1
{
  "file_path": "/User/alice/photos/2017/05/16/my_photo1.jpg"
}

POST file-path-test/_doc/2
{
  "file_path": "/User/alice/photos/2017/05/16/my_photo2.jpg"
}

POST file-path-test/_doc/3
{
  "file_path": "/User/alice/photos/2017/05/16/my_photo3.jpg"
}

POST file-path-test/_doc/4
{
  "file_path": "/User/alice/photos/2017/05/15/my_photo1.jpg"
}

POST file-path-test/_doc/5
{
  "file_path": "/User/bob/photos/2017/05/16/my_photo1.jpg"
}
```

A search for a particular file path string against the text field matches all the example documents, with Bob’s documents ranking highest due to `bob` also being one of the terms created by the standard analyzer boosting relevance for Bob’s documents.

```console
GET file-path-test/_search
{
  "query": {
    "match": {
      "file_path": "/User/bob/photos/2017/05"
    }
  }
}
```

It’s simple to match or filter documents with file paths that exist within a particular directory using the `file_path.tree` field.

```console
GET file-path-test/_search
{
  "query": {
    "term": {
      "file_path.tree": "/User/alice/photos/2017/05/16"
    }
  }
}
```

With the reverse parameter for this tokenizer, it’s also possible to match from the other end of the file path, such as individual file names or a deep level subdirectory. The following example shows a search for all files named `my_photo1.jpg` within any directory via the `file_path.tree_reversed` field configured to use the reverse parameter in the mapping.

```console
GET file-path-test/_search
{
  "query": {
    "term": {
      "file_path.tree_reversed": {
        "value": "my_photo1.jpg"
      }
    }
  }
}
```

Viewing the tokens generated with both forward and reverse is instructive in showing the tokens created for the same file path value.

```console
POST file-path-test/_analyze
{
  "analyzer": "custom_path_tree",
  "text": "/User/alice/photos/2017/05/16/my_photo1.jpg"
}

POST file-path-test/_analyze
{
  "analyzer": "custom_path_tree_reversed",
  "text": "/User/alice/photos/2017/05/16/my_photo1.jpg"
}
```

It’s also useful to be able to filter with file paths when combined with other types of searches, such as this example looking for any files paths with `16` that also must be in Alice’s photo directory.

```console
GET file-path-test/_search
{
  "query": {
    "bool" : {
      "must" : {
        "match" : { "file_path" : "16" }
      },
      "filter": {
        "term" : { "file_path.tree" : "/User/alice" }
      }
    }
  }
}
```

