import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { styles } from './styles';
import UserParticipants from './user-participants/component';
import UserMessages from './user-messages/component';

const propTypes = {
  openChats: PropTypes.arrayOf(String).isRequired,
  users: PropTypes.arrayOf(Object).isRequired,
  compact: PropTypes.bool,
  intl: PropTypes.shape({
    formatMessage: PropTypes.func.isRequired,
  }).isRequired,
  currentUser: PropTypes.shape({}).isRequired,
  meeting: PropTypes.shape({}).isRequired,
  isBreakoutRoom: PropTypes.bool,
  getAvailableActions: PropTypes.func.isRequired,
  normalizeEmojiName: PropTypes.func.isRequired,
  isMeetingLocked: PropTypes.func.isRequired,
  isPublicChat: PropTypes.func.isRequired,
  setEmojiStatus: PropTypes.func.isRequired,
  assignPresenter: PropTypes.func.isRequired,
  kickUser: PropTypes.func.isRequired,
  toggleVoice: PropTypes.func.isRequired,
  changeRole: PropTypes.func.isRequired,
  roving: PropTypes.func.isRequired,
};

const defaultProps = {
  compact: false,
  isBreakoutRoom: false,
  // This one is kinda tricky, meteor takes sometime to fetch the data and passing down
  // So the first time its create, the meeting comes as null, sending an error to the client.
  meeting: {},
};

class UserContent extends Component {
  render() {
    return (
      <div className={styles.content}>
        <UserMessages
          isPublicChat={this.props.isPublicChat}
          openChats={this.props.openChats}
          compact={this.props.compact}
          intl={this.props.intl}
          roving={this.props.roving}
          isChatEnabled={this.props.isChatEnabled}
          meeting={this.props.meeting}
        />
        <UserParticipants
          users={this.props.users}
          compact={this.props.compact}
          intl={this.props.intl}
          currentUser={this.props.currentUser}
          meeting={this.props.meeting}
          isBreakoutRoom={this.props.isBreakoutRoom}
          setEmojiStatus={this.props.setEmojiStatus}
          assignPresenter={this.props.assignPresenter}
          kickUser={this.props.kickUser}
          toggleVoice={this.props.toggleVoice}
          changeRole={this.props.changeRole}
          getAvailableActions={this.props.getAvailableActions}
          normalizeEmojiName={this.props.normalizeEmojiName}
          isMeetingLocked={this.props.isMeetingLocked}
          roving={this.props.roving}
        />
      </div>
    );
  }
}

UserContent.propTypes = propTypes;
UserContent.defaultProps = defaultProps;

export default UserContent;
