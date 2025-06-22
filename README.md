# File-Organizing Multi-Agent System

This project is a multi-agent system built using the JADE (Java Agent DEvelopment Framework) platform to automate file organization. It scans a source directory, classifies files based on their extensions, and sorts them into categorized folders in a target directory. The system leverages a distributed architecture with specialized agents that collaborate through message passing to achieve the task.

## Overview

The system consists of five types of agents, each with a distinct role in the file organization process. Agents register their services with JADE's Directory Facilitator (DF) for discovery and communicate using ACL (Agent Communication Language) messages. The workflow involves scanning files, classifying them by type, and moving them to appropriate folders based on predefined categories.

## Agent Architecture

The system comprises the following agents, each responsible for a specific part of the file organization process:

### 1. GUI Agent
- **Role**: Acts as the system coordinator, initiating the process by providing source and target directory paths to other agents.
- **Service**: Registers as a `gui-boss` service in the DF.
- **Functionality**:
    - Accepts source folder, target folder, and deep scan (recursive or top-level) options as input.
    - Searches for `sorting` and `monitor` services to delegate tasks.
    - Sends the target folder to the Sorting Agent and the source folder (with scan options) to the Monitor Agent.
- **Communication**:
    - Sends `INFORM` messages with conversation ID `set-folder` to configure other agents.
    - Listens for `CONFIRM` messages to acknowledge task completion or settings.

### 2. Monitor Agent
- **Role**: Scans the source directory for files and sends the file list to the classification coordinator.
- **Service**: Registers as a `monitor` service in the DF.
- **Functionality**:
    - Receives the source folder and deep scan option from the GUI Agent.
    - Scans the directory (recursively or top-level) using Java NIO and maintains a set of unique file paths.
    - Sends the collected file list to the Classifier Manager for processing.
- **Communication**:
    - Receives `INFORM` messages with conversation ID `set-folder` to set the directory.
    - Sends `CONFIRM` messages with conversation ID `folder-set` to acknowledge directory settings.
    - Sends `INFORM` messages with conversation ID `file-list` containing comma-separated file paths.

### 3. Classifier Manager Agent
- **Role**: Coordinates file classification by distributing files among worker agents.
- **Service**: Registers as a `classification-coordinator` service in the DF.
- **Functionality**:
    - Receives the file list from the Monitor Agent and splits it into chunks (up to 100 files per worker).
    - Dynamically creates Classifier Worker Agents to process file subsets.
    - Loads a file extension-to-category mapping from a CSV file for classification.
- **Communication**:
    - Receives `INFORM` messages with conversation ID `file-list` containing file paths.
    - Sends `CONFIRM` messages with conversation ID `scan-request-processed` to acknowledge file list receipt.
    - Receives `INFORM` messages with conversation ID `worker-finished-notice` from workers upon completion.

### 4. Classifier Worker Agent
- **Role**: Classifies files based on their extensions and delegates specialized processing or sorting.
- **Service**: Does not register a service (transient agent created by Classifier Manager).
- **Functionality**:
    - Processes a subset of files assigned by the Classifier Manager.
    - Categorizes files by extension using the shared extension-to-category mapping.
    - Searches for specialized classifiers (e.g., `classifier-image`) for specific categories and delegates files if found.
    - Sends categorized file lists to the Sorting Agent for physical sorting.
    - Terminates after completing its task.
- **Communication**:
    - Receives file lists as agent arguments during creation.
    - Sends `REQUEST` messages with conversation ID `file-list` to specialized classifiers (if any).
    - Sends `INFORM` messages with conversation ID `file-sorting-request` containing JSON-encoded categorized file lists to the Sorting Agent.
    - Sends `INFORM` messages with conversation ID `worker-finished-notice` to the Classifier Manager upon termination.

### 5. Sorting Agent
- **Role**: Physically moves files into categorized subfolders in the target directory.
- **Service**: Registers as a `sorting` service in the DF.
- **Functionality**:
    - Receives the target folder from the GUI Agent and categorized file lists from Classifier Worker Agents.
    - Creates subfolders for each category in the target directory.
    - Moves files to their respective category folders using Java NIO.
    - Notifies the GUI Agent upon completion.
- **Communication**:
    - Receives `INFORM` messages with conversation ID `set-folder` to set the target directory.
    - Sends `CONFIRM` messages with conversation ID `folder-set` to acknowledge directory settings.
    - Receives `INFORM` messages with conversation ID `file-sorting-request` containing JSON-encoded file categories.
    - Sends `CONFIRM` messages with conversation ID `files-sorted` to the GUI Agent to confirm sorting completion.

## Communication Protocol

Agents communicate using JADE's ACL messages with specific performatives, conversation IDs, and content formats:

- **Performatives**:
    - `INFORM`: Used to send data (e.g., folder paths, file lists).
    - `CONFIRM`: Used to acknowledge receipt or completion of tasks.
    - `REQUEST`: Used by Classifier Worker Agents to delegate to specialized classifiers.

- **Conversation IDs**:
    - `set-folder`: For setting source or target directories.
    - `folder-set`: For confirming directory settings.
    - `file-list`: For sending file lists between agents.
    - `scan-request-processed`: For confirming file list receipt.
    - `file-sorting-request`: For sending categorized file lists to the Sorting Agent.
    - `files-sorted`: For confirming file sorting completion.
    - `worker-finished-notice`: For notifying the Classifier Manager of worker completion.

- **Content Formats**:
    - Folder settings: Plain text (e.g., `/path/to/folder` or `/path/to/folder,true` for deep scan).
    - File lists: Comma-separated file paths or JSON-encoded category-to-file mappings.
    - Confirmations: Descriptive text summarizing the action.

- **Service Discovery**:
    - Agents register services with the DF (e.g., `gui-boss`, `monitor`, `sorting`, `classification-coordinator`).
    - Agents search the DF to locate other services dynamically, enabling flexible collaboration.

## Workflow Summary

1. The **GUI Agent** starts the process by sending the source folder (with scan option) to the **Monitor Agent** and the target folder to the **Sorting Agent**.
2. The **Monitor Agent** scans the source directory and sends a file list to the **Classifier Manager**.
3. The **Classifier Manager** splits the file list and creates **Classifier Worker Agents** to process subsets.
4. Each **Classifier Worker Agent** categorizes files, delegates to specialized classifiers if available, and sends categorized lists to the **Sorting Agent**.
5. The **Sorting Agent** creates category folders in the target directory, moves files accordingly, and notifies the **GUI Agent** of completion.


## Future Enhancements

- Support for multiple instances of the same agent type with load balancing.
- Configurable parameters (e.g., max files per worker, scan options) via a configuration file.
- Enhanced error handling and recovery mechanisms for robust operation.

---

*This project demonstrates a distributed, agent-based approach to file organization, showcasing JADE's capabilities for building collaborative multi-agent systems.*