(in-ns 'rapidstash.core)
(import '[java.io RandomAccessFile])

"Define metadata"
(def freeSpot 0)
(def numBlocks 0)

(def frame (spec :freeSpot (int32-type)
                 :numBlocks (int32-type)
                 :headerText (string-type 8)))

(defn createLargeFile
   "creates a 2gb file that can be immmediately mapped"
   [filename]
   (doto (RandomAccessFile. filename "rw")
      (.seek (* 1024 1024 1024 2))
      (.writeInt 0)
      (.close)))

(defn writeBuffer
   [file buf spot]
   (let [arr (byte-array (.capacity (.buffer buf)))]
      (.getBytes (.buffer buf) 0 arr)
      (mmap/put-bytes file arr spot)))

(defn writeCollectionHeader
   [file]
   (let [buf (compose-buffer frame)]
      (set-field buf :freeSpot 0)
      (set-field buf :numBlocks 0)
      (set-field buf :headerText "DEADBEEF")
      (writeBuffer file buf 0)))


(defn newCollection
   "creates a new collection and return the memory map"
   [collName]
   (if (not (.exists (clojure.java.io/file collName)))
      (createLargeFile collName))
   (def file (mmap/get-mmap collName :read-write))
   (writeCollectionHeader file)
   file)
