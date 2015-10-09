# Author JSON Format #

```JSON
{ 
  "firstName" : "some string", 
  "middleNames" : [ "some", "list", "of", "strings" ], 
  "lastName" : "some string", 
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