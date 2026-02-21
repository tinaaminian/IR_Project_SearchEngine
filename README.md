## Description
This project implements a flexible search engine using Apache Lucene.
It features standard indexing, k-gram indexing, and word-level bigram indexing to enhance search functionalities and support spell correction suggestions.
The search engine allows users to input queries and retrieves relevant documents, providing spelling suggestions when no exact matches are found.

## Features
- standard Indexing: Indexes documents using a standard analyzer.
- K-gram Indexing: Indexes documents using k-grams for enhanced search capabilities.
- Word-level Bigram Indexing: Generates bigrams from indexed words to improve retrieval accuracy.
- Spell Correction Suggestions: Suggests spelling corrections based on the indexed k-grams.

## Installation
- install JDK 8 or higher
- install Apache Lucene library and make sure to include it in your project.
- install cygwin for windows as this project gets data and result direcotories as input.

## Usage
To run the search engine, use the following command: <br>
java FinalFlexibleLuceneSearchEngine indexDir dataDir  <br>
 indexDir : Directory to store the index <br>
dataDir : Directory containing the text files to be indexed
