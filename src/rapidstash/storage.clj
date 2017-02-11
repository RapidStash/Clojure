(in-ns 'rapidstash.core)
(import '[java.io RandomAccessFile])

"Define metadata"
(def freeSpot 0)
(def numBlocks 0)
(def header (clojure.string/join " " (list freeSpot, numBlocks)))

(defn createLargeFile
   "creates a 2gb file that can be immmediately mapped"
   [filename]
   (doto (RandomAccessFile. filename "rw")
      (.seek (* 1024 1024 1024 2))
      (.writeInt 0)
      (.close)))

(defn newCollection
   "creates a new collection"
   [collName]
   (if (not (.exists (clojure.java.io/file collName)))
      (createLargeFile collName))
   (def file (mmap/get-mmap collName :read-write))
   (let [bytes-to-write (.getBytes header "UTF-8")
         file-size      (.size file)]
     (if (> file-size
            (alength bytes-to-write))
       (mmap/put-bytes file bytes-to-write 0)))
   file)
