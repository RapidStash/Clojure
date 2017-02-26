(in-ns 'rapidstash.core)
(import '[java.io RandomAccessFile])

(def mapSize (* 1024 1024 1024 4))
(def frameSize 2048)
(def collectionHeader nil)

(defn getSpecSize [s] (.capacity (.buffer (compose-buffer s))))

(def collectionHeader (spec :freeSpot (int32-type)
                            :numFrames (int32-type)))

(def collectionHeaderSize (getSpecSize collectionHeader))

(def frameHeader (spec :nextFrame (int32-type)
                       :docId (string-type 36)))

(def frameHeaderSize (getSpecSize frameHeader))

(def frameData (spec :data (string-type (- frameSize frameHeaderSize))))

(def frameDataSize (getSpecSize frameData))

(def frame (concat frameHeader frameData))

(defn getFramePosition 
   [frameId] 
   "gets the file offset of the frame from the given frame Id"
   (+ collectionHeaderSize (* frameId frameSize)))

(defn getNextFreeSpot
   []
   "gets the next free frame Id and increments the nextSpot by 1"
   (def value 0)
   (locking collectionHeader 
      (def value (get-field collectionHeader :freeSpot))
      (set-field collectionHeader :freeSpot (+ value 1)))
   value)

(defn createLargeFile
   [filename]
   "creates a file that can be immmediately mapped"
   (doto (RandomAccessFile. filename "rw")
      (.seek mapSize)
      (.writeInt 0)
      (.close)))

(defn writeBuffer
   [file buf spot]
   (let [arr (byte-array (.capacity (.buffer buf)))]
      (.getBytes (.buffer buf) 0 arr)
      (mmap/put-bytes file arr spot)))

(defn readBuffer
   [file spot frameSpec]
   (def dest (compose-buffer frameSpec))
   (def numBytes (.capacity (.buffer dest)))
   (def source (mmap/get-bytes file spot numBytes))
   (.setBytes (.buffer dest) 0 source 0 numBytes)
   dest)

(defn writeCollectionHeader
   [file freeSpot numFrames]
   "Write the header to the collection"
   (let [buf (compose-buffer collectionHeader)]
      (set-field buf :freeSpot freeSpot)
      (set-field buf :numFrames numFrames)
      (writeBuffer file buf 0)))

(defn readCollectionHeader
   [file]
   "Read the header from the collection file"
   (readBuffer file 0 collectionHeader))

(defn createFrameBuffer
   [nextFrame docId data]
   (def buf (compose-buffer frame))
   (set-field buf :nextFrame nextFrame)
   (set-field buf :docId docId)
   (set-field buf :data data)
   buf) 

(defn openCollection
   "open a collection and return the memory map"
   [collName]
   (def newCollection? false)
   (if (not (.exists (clojure.java.io/file collName)))
     (do
      	(createLargeFile collName)
        (def newCollection? true)))
   (def file (mmap/get-mmap collName :read-write))
   (if (= newCollection? true)
      (writeCollectionHeader file 0 0))
   (def collectionHeader (readCollectionHeader file))
   (println "Collection header")
   (println (decompose collectionHeader))
   (println "Writing two frames")
   (writeBuffer file (createFrameBuffer -1 "abc123" "Hello, World!") (getFramePosition (getNextFreeSpot)))
   (writeBuffer file (createFrameBuffer -1 "abc124" "Herp Derp") (getFramePosition (getNextFreeSpot)))
   (writeBuffer file (createFrameBuffer -1 "abc123" "Hello, World! This is a large amount of data.") (getFramePosition 0))
   (println "Reading the frames")
   (println (decompose (readBuffer file (getFramePosition 0) frame)))
   (println (decompose (readBuffer file (getFramePosition 1) frame)))
   file)
