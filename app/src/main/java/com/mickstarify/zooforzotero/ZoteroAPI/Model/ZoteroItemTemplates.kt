package com.mickstarify.zooforzotero.ZoteroAPI.Model

class ZoteroItemTemplates {
    companion object {
        val ITEM_ARTWORK_JSON = """{
    "itemType": "artwork",
    "title": "",
    "creators": [
        {
            "creatorType": "artist",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "artworkMedium": "",
    "artworkSize": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_AUDIORECORDING_JSON = """{
    "itemType": "audioRecording",
    "title": "",
    "creators": [
        {
            "creatorType": "performer",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "audioRecordingFormat": "",
    "seriesTitle": "",
    "volume": "",
    "numberOfVolumes": "",
    "place": "",
    "label": "",
    "date": "",
    "runningTime": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_BILL_JSON = """{
    "itemType": "bill",
    "title": "",
    "creators": [
        {
            "creatorType": "sponsor",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "billNumber": "",
    "code": "",
    "codeVolume": "",
    "section": "",
    "codePages": "",
    "legislativeBody": "",
    "session": "",
    "history": "",
    "date": "",
    "language": "",
    "url": "",
    "accessDate": "",
    "shortTitle": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_BLOGPOST_JSON = """{
    "itemType": "blogPost",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "blogTitle": "",
    "websiteType": "",
    "date": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "shortTitle": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_BOOK_JSON = """{
    "itemType": "book",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "series": "",
    "seriesNumber": "",
    "volume": "",
    "numberOfVolumes": "",
    "edition": "",
    "place": "",
    "publisher": "",
    "date": "",
    "numPages": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_BOOKSECTION_JSON = """{
    "itemType": "bookSection",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "bookTitle": "",
    "series": "",
    "seriesNumber": "",
    "volume": "",
    "numberOfVolumes": "",
    "edition": "",
    "place": "",
    "publisher": "",
    "date": "",
    "pages": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_CASE_JSON = """{
    "itemType": "case",
    "caseName": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "court": "",
    "dateDecided": "",
    "docketNumber": "",
    "reporter": "",
    "reporterVolume": "",
    "firstPage": "",
    "history": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_COMPUTERPROGRAM_JSON = """{
    "itemType": "computerProgram",
    "title": "",
    "creators": [
        {
            "creatorType": "programmer",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "seriesTitle": "",
    "versionNumber": "",
    "date": "",
    "system": "",
    "place": "",
    "company": "",
    "programmingLanguage": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "rights": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "accessDate": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_CONFERENCEPAPER_JSON = """{
    "itemType": "conferencePaper",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "date": "",
    "proceedingsTitle": "",
    "conferenceName": "",
    "place": "",
    "publisher": "",
    "volume": "",
    "pages": "",
    "series": "",
    "language": "",
    "DOI": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_DICTIONARYENTRY_JSON = """{
    "itemType": "dictionaryEntry",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "dictionaryTitle": "",
    "series": "",
    "seriesNumber": "",
    "volume": "",
    "numberOfVolumes": "",
    "edition": "",
    "place": "",
    "publisher": "",
    "date": "",
    "pages": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_DOCUMENT_JSON = """{
    "itemType": "document",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "publisher": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_EMAIL_JSON = """{
    "itemType": "email",
    "subject": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "date": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_ENCYCLOPEDIAARTICLE_JSON = """{
    "itemType": "encyclopediaArticle",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "encyclopediaTitle": "",
    "series": "",
    "seriesNumber": "",
    "volume": "",
    "numberOfVolumes": "",
    "edition": "",
    "place": "",
    "publisher": "",
    "date": "",
    "pages": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_FILM_JSON = """{
    "itemType": "film",
    "title": "",
    "creators": [
        {
            "creatorType": "director",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "distributor": "",
    "date": "",
    "genre": "",
    "videoRecordingFormat": "",
    "runningTime": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_FORUMPOST_JSON = """{
    "itemType": "forumPost",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "forumTitle": "",
    "postType": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_HEARING_JSON = """{
    "itemType": "hearing",
    "title": "",
    "creators": [
        {
            "creatorType": "contributor",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "committee": "",
    "place": "",
    "publisher": "",
    "numberOfVolumes": "",
    "documentNumber": "",
    "pages": "",
    "legislativeBody": "",
    "session": "",
    "history": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_INSTANTMESSAGE_JSON = """{
    "itemType": "instantMessage",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_INTERVIEW_JSON = """{
    "itemType": "interview",
    "title": "",
    "creators": [
        {
            "creatorType": "interviewee",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "date": "",
    "interviewMedium": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_JOURNALARTICLE_JSON = """{
    "itemType": "journalArticle",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "publicationTitle": "",
    "volume": "",
    "issue": "",
    "pages": "",
    "date": "",
    "series": "",
    "seriesTitle": "",
    "seriesText": "",
    "journalAbbreviation": "",
    "language": "",
    "DOI": "",
    "ISSN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_LETTER_JSON = """{
    "itemType": "letter",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "letterType": "",
    "date": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_MAGAZINEARTICLE_JSON = """{
    "itemType": "magazineArticle",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "publicationTitle": "",
    "volume": "",
    "issue": "",
    "date": "",
    "pages": "",
    "language": "",
    "ISSN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_MANUSCRIPT_JSON = """{
    "itemType": "manuscript",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "manuscriptType": "",
    "place": "",
    "date": "",
    "numPages": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_MAP_JSON = """{
    "itemType": "map",
    "title": "",
    "creators": [
        {
            "creatorType": "cartographer",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "mapType": "",
    "scale": "",
    "seriesTitle": "",
    "edition": "",
    "place": "",
    "publisher": "",
    "date": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_NEWSPAPERARTICLE_JSON = """{
    "itemType": "newspaperArticle",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "publicationTitle": "",
    "place": "",
    "edition": "",
    "date": "",
    "section": "",
    "pages": "",
    "language": "",
    "shortTitle": "",
    "ISSN": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_NOTE_JSON = """{
    "itemType": "note",
    "note": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_PATENT_JSON = """{
    "itemType": "patent",
    "title": "",
    "creators": [
        {
            "creatorType": "inventor",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "place": "",
    "country": "",
    "assignee": "",
    "issuingAuthority": "",
    "patentNumber": "",
    "filingDate": "",
    "pages": "",
    "applicationNumber": "",
    "priorityNumbers": "",
    "issueDate": "",
    "references": "",
    "legalStatus": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_PODCAST_JSON = """{
    "itemType": "podcast",
    "title": "",
    "creators": [
        {
            "creatorType": "podcaster",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "seriesTitle": "",
    "episodeNumber": "",
    "audioFileType": "",
    "runningTime": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "shortTitle": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_PRESENTATION_JSON = """{
    "itemType": "presentation",
    "title": "",
    "creators": [
        {
            "creatorType": "presenter",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "presentationType": "",
    "date": "",
    "place": "",
    "meetingName": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "shortTitle": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_RADIOBROADCAST_JSON = """{
    "itemType": "radioBroadcast",
    "title": "",
    "creators": [
        {
            "creatorType": "director",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "programTitle": "",
    "episodeNumber": "",
    "audioRecordingFormat": "",
    "place": "",
    "network": "",
    "date": "",
    "runningTime": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_REPORT_JSON = """{
    "itemType": "report",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "reportNumber": "",
    "reportType": "",
    "seriesTitle": "",
    "place": "",
    "institution": "",
    "date": "",
    "pages": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_STATUTE_JSON = """{
    "itemType": "statute",
    "nameOfAct": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "code": "",
    "codeNumber": "",
    "publicLawNumber": "",
    "dateEnacted": "",
    "pages": "",
    "section": "",
    "session": "",
    "history": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_TVBROADCAST_JSON = """{
    "itemType": "tvBroadcast",
    "title": "",
    "creators": [
        {
            "creatorType": "director",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "programTitle": "",
    "episodeNumber": "",
    "videoRecordingFormat": "",
    "place": "",
    "network": "",
    "date": "",
    "runningTime": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_THESIS_JSON = """{
    "itemType": "thesis",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "thesisType": "",
    "university": "",
    "place": "",
    "date": "",
    "numPages": "",
    "language": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_VIDEORECORDING_JSON = """{
    "itemType": "videoRecording",
    "title": "",
    "creators": [
        {
            "creatorType": "director",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "videoRecordingFormat": "",
    "seriesTitle": "",
    "volume": "",
    "numberOfVolumes": "",
    "place": "",
    "studio": "",
    "date": "",
    "runningTime": "",
    "language": "",
    "ISBN": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "archive": "",
    "archiveLocation": "",
    "libraryCatalog": "",
    "callNumber": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

        val ITEM_WEBPAGE_JSON = """{
    "itemType": "webpage",
    "title": "",
    "creators": [
        {
            "creatorType": "author",
            "firstName": "",
            "lastName": ""
        }
    ],
    "abstractNote": "",
    "websiteTitle": "",
    "websiteType": "",
    "date": "",
    "shortTitle": "",
    "url": "",
    "accessDate": "",
    "language": "",
    "rights": "",
    "extra": "",
    "tags": [],
    "collections": [],
    "relations": {}
}"""

    }
}