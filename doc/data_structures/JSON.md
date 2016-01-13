# Author JSON Format #

```JSON
{ 
  "firstName" : "some string", 
  "middleNames" : [ "some", "list", "of", "strings" ], 
  "lastName" : "some string",
  "suffixes" : [ "some", "list", "of", "strings"],
  "emails" : [ "some", "list", "of", "strings" ], 
  "institutions" : [ "some", "list", "of", "strings" ]
}
```

# Venue JSON Format #

```JSON
{
  "name": "some string"
}
```

# Author Mention JSON Format #


```JSON
{
  "self": Author record,
  "coauthors": list of Author records,
  "title": "some string",
  "titleEmbeddingKeywords": [ "some", "list", "of", "strings" ],
  "topics": [ "some", "list", "of", "strings" ],
  "text": "some string",
  "tokenizedText": [ "some", "list", "of", "strings" ],
  "venues": list of Venue records,
  "keywords": [ "some", "list", "of", "strings" ],
  "canopies": ["some", "list", "of", "strings" ],
  "canopy": "some string",
  "source": "some string"
}
```

# Additional Notes #

- Suffixes are not currently being used by the system

- Missing values can just be left out of the JSON object

- Canopy need not be specified here for most applications. As this will be determined in the coref-task objects and the execution of the coreference itself.

- Source is only provided for book-keeping, i.e. if the origin of a record is wished to be stored.


# Example #


```
{"self" : {"firstName" : "John", "middleNames" : [ "Michael"],"lastName" : "Smith", "institutions" : [ "University of South Carolina" ]},"coauthors" : [{"firstName" : "Jack", "lastName" : "Douglas", "institutions" : [ "University of South Carolina" ]},{"firstName" : "Alice", "lastName" : "Peters", "institutions" : [ "University of South Carolina" ]}],"title": "Probabilistic Graphical Models","titleEmbeddingKeywords": ["Probabilistic", "Graphical","Models"],"text": "These notes provide an overview of discriminative and generative graphical models","tokenizedText": ["These", "notes", "provide", "an", "overview", "of", "discriminative", "and", "generative", "graphical", "models"],"venues": [{"name" : "Tech Report"}]}
```