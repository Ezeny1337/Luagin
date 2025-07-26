import React, { useState, useEffect } from 'react';
import { Form, Input, InputNumber, Button, Card, Space, Typography, Alert, Tag, Table, Modal, Select, message } from 'antd';
import { SaveOutlined, PlusOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;
const { Option } = Select;

interface Group {
  weight: number;
  permissions: string[];
  inherit?: string[];
}

interface Player {
  groups?: string[];
  permissions?: string[];
}

const PermissionsConfig: React.FC = () => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [groups, setGroups] = useState<Record<string, Group>>({});
  const [players, setPlayers] = useState<Record<string, Player>>({});
  const [selectedPlayer, setSelectedPlayer] = useState<string>('');
  const [playerModalVisible, setPlayerModalVisible] = useState(false);
  const [groupModalVisible, setGroupModalVisible] = useState(false);
  const [editingGroup, setEditingGroup] = useState<string>('');
  const [newPermission, setNewPermission] = useState<string>('');
  const [loadingGroups, setLoadingGroups] = useState(false);
  const [loadingPlayers, setLoadingPlayers] = useState(false);

  useEffect(() => {
    fetchPermissionsConfig();
  }, []);

  // 获取权限配置
  const fetchPermissionsConfig = async () => {
    try {
      const [groupsResponse, playersResponse] = await Promise.all([
        fetch('/api/permissions/groups', { credentials: 'include' }),
        fetch('/api/permissions/players', { credentials: 'include' })
      ]);

      if (groupsResponse.ok && playersResponse.ok) {
        const groupsData = await groupsResponse.json();
        const playersData = await playersResponse.json();

        setGroups(groupsData);
        setPlayers(playersData);
      } else {
        message.error(t('config.fetchFailed'));
      }
    } catch (error) {
      message.error(t('config.fetchFailed'));
    }
  };

  // 权限组管理
  const addGroup = () => {
    setEditingGroup('');
    setGroupModalVisible(true);
  };

  const editGroup = (groupName: string) => {
    setEditingGroup(groupName);
    setGroupModalVisible(true);
  };

  const removeGroup = async (groupName: string) => {
    try {
      const response = await fetch(`/api/permissions/group/${groupName}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        const updatedGroups = { ...groups };
        delete updatedGroups[groupName];
        setGroups(updatedGroups);
        message.success(t('config.saveSuccess'));
      } else {
        message.error(t('config.saveFailed'));
      }
    } catch (error) {
      console.error('Remove group failed:', error);
      message.error(t('config.saveFailed'));
    }
  };

  const updateGroupLocal = (groupName: string, groupData: Group) => {
    const updatedGroups = { ...groups };
    updatedGroups[groupName] = groupData;
    setGroups(updatedGroups);
    setGroupModalVisible(false);
  };

  const saveGroup = async (groupName: string, groupData: Group) => {
    try {
      const response = await fetch('/api/permissions/group', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          name: groupName,
          permissions: groupData.permissions,
          weight: groupData.weight,
          inherit: groupData.inherit || []
        })
      });

      if (response.ok) {
        const updatedGroups = { ...groups };
        updatedGroups[groupName] = groupData;
        setGroups(updatedGroups);
        message.success(t('config.saveSuccess'));
      } else {
        message.error(t('config.saveFailed'));
      }
    } catch (error) {
      console.error('Save group failed:', error);
      message.error(t('config.saveFailed'));
    }
  };

  // 玩家管理
  const addPlayer = () => {
    setSelectedPlayer('');
    setPlayerModalVisible(true);
  };

  const editPlayer = (playerName: string) => {
    setSelectedPlayer(playerName);
    setPlayerModalVisible(true);
  };

  const removePlayer = async (playerName: string) => {
    try {
      const response = await fetch(`/api/permissions/player/${playerName}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        const updatedPlayers = { ...players };
        delete updatedPlayers[playerName];
        setPlayers(updatedPlayers);
        message.success(t('config.saveSuccess'));
      } else {
        message.error(t('config.saveFailed'));
      }
    } catch (error) {
      console.error('Remove player failed:', error);
      message.error(t('config.saveFailed'));
    }
  };

  const updatePlayerLocal = (playerName: string, playerData: Player) => {
    const updatedPlayers = { ...players };
    updatedPlayers[playerName] = playerData;
    setPlayers(updatedPlayers);
    setPlayerModalVisible(false);
  };

  const savePlayer = async (playerName: string, playerData: Player) => {
    try {
      const response = await fetch('/api/permissions/player', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          name: playerName,
          groups: playerData.groups || [],
          permissions: playerData.permissions || []
        })
      });

      if (response.ok) {
        const updatedPlayers = { ...players };
        updatedPlayers[playerName] = playerData;
        setPlayers(updatedPlayers);
        message.success(t('config.saveSuccess'));
      } else {
        message.error(t('config.saveFailed'));
      }
    } catch (error) {
      console.error('Save player failed:', error);
      message.error(t('config.saveFailed'));
    }
  };

  // 权限管理
  const addPermissionToGroup = async (groupName: string, permission: string) => {
    const group = groups[groupName];
    if (group && permission.trim()) {
      const updatedPermissions = [...group.permissions, permission.trim()];
      const updatedGroup = { ...group, permissions: updatedPermissions };
      await saveGroup(groupName, updatedGroup);
    }
  };

  const addPermissionToPlayer = async (playerName: string, permission: string) => {
    const player = players[playerName] || {};
    const permissions = player.permissions || [];
    if (permission.trim()) {
      const updatedPermissions = [...permissions, permission.trim()];
      const updatedPlayer = { ...player, permissions: updatedPermissions };
      await savePlayer(playerName, updatedPlayer);
    }
  };

  const removePermissionFromPlayer = async (playerName: string, permissionIndex: number) => {
    const player = players[playerName];
    if (player && player.permissions) {
      const updatedPermissions = player.permissions.filter((_, index) => index !== permissionIndex);
      const updatedPlayer = { ...player, permissions: updatedPermissions };
      await savePlayer(playerName, updatedPlayer);
    }
  };

  const addPlayerToGroup = async (playerName: string, groupName: string) => {
    const player = players[playerName] || {};
    const groups = player.groups || [];
    if (!groups.includes(groupName)) {
      const updatedGroups = [...groups, groupName];
      const updatedPlayer = { ...player, groups: updatedGroups };
      await savePlayer(playerName, updatedPlayer);
    }
  };

  const removePlayerFromGroup = async (playerName: string, groupName: string) => {
    const player = players[playerName];
    if (player && player.groups) {
      const updatedGroups = player.groups.filter(g => g !== groupName);
      const updatedPlayer = { ...player, groups: updatedGroups };
      await savePlayer(playerName, updatedPlayer);
    }
  };

  const cardStyle = {
    background: '#fff',
    borderRadius: 8,
    padding: 24,
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #e5e6eb',
    marginBottom: 16,
  };

  // 表格列定义
  const groupColumns = [
    {
      title: t('config.permissionsConfig.groupName'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('config.permissionsConfig.weight'),
      dataIndex: 'weight',
      key: 'weight',
      render: (weight: number, record: any) => (
        <InputNumber
          value={weight}
          onChange={(value) => {
            const group = groups[record.name];
            if (group) {
              const updatedGroup = { ...group, weight: value || 0 };
              setGroups({ ...groups, [record.name]: updatedGroup });
            }
          }}
          min={0}
          max={1000}
          style={{ width: 80 }}
        />
      ),
    },
    {
      title: t('config.permissionsConfig.permissions'),
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissions: string[]) => (
        <div>
          {permissions.map((permission, index) => (
            <Tag key={index} style={{ marginBottom: 4 }}>
              {permission}
            </Tag>
          ))}
        </div>
      ),
    },
    {
      title: t('config.permissionsConfig.inherit'),
      dataIndex: 'inherit',
      key: 'inherit',
      render: (inherit: string[]) => (
        <div>
          {inherit?.map((group, index) => (
            <Tag key={index} color="blue">
              {group}
            </Tag>
          )) || '-'}
        </div>
      ),
    },
    {
      title: t('config.permissionsConfig.actions'),
      key: 'actions',
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => editGroup(record.name)}
          >
            {t('config.permissionsConfig.edit')}
          </Button>
          <Button
            type="link"
            danger
            size="small"
            onClick={async () => await removeGroup(record.name)}
          >
            {t('config.permissionsConfig.delete')}
          </Button>
        </Space>
      ),
    },
  ];

  const playerColumns = [
    {
      title: t('config.permissionsConfig.playerName'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('config.permissionsConfig.groups'),
      dataIndex: 'groups',
      key: 'groups',
      render: (groups: string[], record: any) => (
        <div>
          {groups?.map((group, index) => (
            <Tag
              key={index}
              closable
              onClose={async () => await removePlayerFromGroup(record.name, group)}
              color="green"
            >
              {group}
            </Tag>
          )) || '-'}
          <Select
            placeholder={t('config.permissionsConfig.addToGroup')}
            style={{ width: 120, marginLeft: 8 }}
            onSelect={async (groupName) => await addPlayerToGroup(record.name, groupName)}
          >
            {Object.keys(groups).map(groupName => (
              <Option key={groupName} value={groupName}>{groupName}</Option>
            ))}
          </Select>
        </div>
      ),
    },
    {
      title: t('config.permissionsConfig.permissions'),
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissions: string[], record: any) => (
        <div>
          {permissions?.map((permission, index) => (
            <Tag
              key={index}
              closable
              onClose={async () => await removePermissionFromPlayer(record.name, index)}
              style={{ marginBottom: 4 }}
            >
              {permission}
            </Tag>
          )) || '-'}
          <Button
            type="dashed"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => {
              setSelectedPlayer(record.name);
              setNewPermission('');
            }}
            style={{ marginLeft: 8 }}
          >
            {t('config.permissionsConfig.addPermission')}
          </Button>
        </div>
      ),
    },
    {
      title: t('config.permissionsConfig.actions'),
      key: 'actions',
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => editPlayer(record.name)}
          >
            {t('config.permissionsConfig.edit')}
          </Button>
          <Button
            type="link"
            danger
            size="small"
            onClick={async () => await removePlayer(record.name)}
          >
            {t('config.permissionsConfig.delete')}
          </Button>
        </Space>
      ),
    },
  ];

  const handleSaveGroups = async () => {
    setLoadingGroups(true);
    try {
      for (const [groupName, groupData] of Object.entries(groups)) {
        await fetch('/api/permissions/group', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            name: groupName,
            permissions: groupData.permissions,
            weight: groupData.weight,
            inherit: groupData.inherit || []
          })
        });
      }
      message.success(t('config.saveSuccess'));
    } catch (error) {
      message.error(t('config.saveFailed'));
    } finally {
      setLoadingGroups(false);
    }
  };

  const handleSavePlayers = async () => {
    setLoadingPlayers(true);
    try {
      for (const [playerName, playerData] of Object.entries(players)) {
        await fetch('/api/permissions/player', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            name: playerName,
            groups: playerData.groups || [],
            permissions: playerData.permissions || []
          })
        });
      }
      message.success(t('config.saveSuccess'));
    } catch (error) {
      message.error(t('config.saveFailed'));
    } finally {
      setLoadingPlayers(false);
    }
  };

  return (
    <div>
      <Alert
        message={t('config.permissionsConfig.description')}
        description={t('config.permissionsConfig.descriptionDetail')}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          groups: {},
          players: {},
        }}
      >
        {/* 权限组管理 */}
        <Card title={t('config.permissionsConfig.groups')} style={cardStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <Title level={4}>
              <TeamOutlined style={{ marginRight: 8 }} />
              {t('config.permissionsConfig.groups')}
            </Title>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={addGroup}
            >
              {t('config.permissionsConfig.addGroup')}
            </Button>
          </div>
          <Table
            dataSource={Object.entries(groups).map(([name, data]) => ({
              key: name,
              name,
              ...data
            }))}
            columns={groupColumns}
            pagination={false}
            size="small"
          />
          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSaveGroups}
              loading={loadingGroups}
            >
              {t('config.permissionsConfig.saveGroups')}
            </Button>
          </div>
        </Card>

        {/* 玩家管理 */}
        <Card title={t('config.permissionsConfig.players')} style={cardStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <Title level={4}>
              <UserOutlined style={{ marginRight: 8 }} />
              {t('config.permissionsConfig.players')}
            </Title>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={addPlayer}
            >
              {t('config.permissionsConfig.addPlayer')}
            </Button>
          </div>
          <Table
            dataSource={Object.entries(players).map(([name, data]) => ({
              key: name,
              name,
              ...data
            }))}
            columns={playerColumns}
            pagination={false}
            size="small"
          />
          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSavePlayers}
              loading={loadingPlayers}
            >
              {t('config.permissionsConfig.savePlayers')}
            </Button>
          </div>
        </Card>
      </Form>

      {/* 权限组编辑模态框 */}
      <Modal
        title={editingGroup ? t('config.permissionsConfig.editGroup') : t('config.permissionsConfig.addGroup')}
        open={groupModalVisible}
        onCancel={() => setGroupModalVisible(false)}
        footer={null}
        width={600}
      >
        <GroupEditForm
          groupName={editingGroup}
          groupData={editingGroup ? groups[editingGroup] : undefined}
          availableGroups={groups}
          onSave={updateGroupLocal}
          onCancel={() => setGroupModalVisible(false)}
        />
      </Modal>

      {/* 玩家编辑模态框 */}
      <Modal
        title={selectedPlayer ? t('config.permissionsConfig.editPlayer') : t('config.permissionsConfig.addPlayer')}
        open={playerModalVisible}
        onCancel={() => setPlayerModalVisible(false)}
        footer={null}
        width={600}
      >
        <PlayerEditForm
          playerName={selectedPlayer}
          playerData={selectedPlayer ? players[selectedPlayer] : undefined}
          availableGroups={Object.keys(groups)}
          onSave={updatePlayerLocal}
          onCancel={() => setPlayerModalVisible(false)}
        />
      </Modal>

      {/* 添加权限模态框 */}
      <Modal
        title={t('config.permissionsConfig.addPermission')}
        open={!!newPermission}
        onCancel={() => setNewPermission('')}
        onOk={async () => {
          if (editingGroup && newPermission.trim()) {
            await addPermissionToGroup(editingGroup, newPermission);
            setNewPermission('');
          } else if (selectedPlayer && newPermission.trim()) {
            await addPermissionToPlayer(selectedPlayer, newPermission);
            setNewPermission('');
          }
        }}
        width={400}
      >
        <Input
          placeholder={t('config.permissionsConfig.permissionPlaceholder')}
          value={newPermission}
          onChange={(e) => setNewPermission(e.target.value)}
          onPressEnter={async () => {
            if (editingGroup && newPermission.trim()) {
              await addPermissionToGroup(editingGroup, newPermission);
              setNewPermission('');
            } else if (selectedPlayer && newPermission.trim()) {
              await addPermissionToPlayer(selectedPlayer, newPermission);
              setNewPermission('');
            }
          }}
        />
      </Modal>
    </div>
  );
};

// 权限组编辑表单组件
interface GroupEditFormProps {
  groupName: string;
  groupData?: Group;
  availableGroups: Record<string, Group>;
  onSave: (name: string, data: Group) => void;
  onCancel: () => void;
}

const GroupEditForm: React.FC<GroupEditFormProps> = ({ groupName, groupData, availableGroups, onSave, onCancel }) => {
  const { t } = useTranslation();
  const [name, setName] = useState(groupName || '');
  const [weight, setWeight] = useState(groupData?.weight || 0);
  const [permissions, setPermissions] = useState<string[]>(groupData?.permissions || []);
  const [inherit, setInherit] = useState<string[]>(groupData?.inherit || []);

  // 同步 props 变化
  useEffect(() => {
    setName(groupName || '');
  }, [groupName]);
  useEffect(() => {
    setWeight(groupData?.weight || 0);
    setPermissions(groupData?.permissions || []);
    setInherit(groupData?.inherit || []);
  }, [groupData]);

  // 更新 state
  const handleOk = () => {
    if (!name.trim()) {
      message.error(t('config.permissionsConfig.groupNameRequired'));
      return;
    }
    onSave(name.trim(), {
      weight,
      permissions,
      inherit,
    });
  };

  // 获取可用的继承选项
  const availableInheritOptions = Object.keys(availableGroups).filter(groupName => groupName !== name);

  return (
    <Form layout="vertical">
      <Form.Item label={t('config.permissionsConfig.groupName')} required>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t('config.permissionsConfig.groupNamePlaceholder')}
        />
      </Form.Item>

      <Form.Item label={t('config.permissionsConfig.weight')}>
        <InputNumber
          value={weight}
          onChange={(value) => setWeight(value || 0)}
          min={0}
          max={1000}
          style={{ width: '100%' }}
        />
      </Form.Item>

      <Form.Item label={t('config.permissionsConfig.permissions')}>
        <div style={{ marginBottom: 8 }}>
          {permissions.map((permission, index) => (
            <Tag
              key={index}
              closable
              onClose={() => setPermissions(permissions.filter((_, i) => i !== index))}
              style={{ marginBottom: 4 }}
            >
              {permission}
            </Tag>
          ))}
        </div>
        <Input
          placeholder={t('config.permissionsConfig.permissionPlaceholder')}
          onPressEnter={(e) => {
            const value = (e.target as HTMLInputElement).value.trim();
            if (value && !permissions.includes(value)) {
              setPermissions([...permissions, value]);
              (e.target as HTMLInputElement).value = '';
            }
          }}
        />
      </Form.Item>

      <Form.Item label={t('config.permissionsConfig.inherit')}>
        <Select
          mode="multiple"
          value={inherit}
          onChange={setInherit}
          placeholder={t('config.permissionsConfig.inheritPlaceholder')}
          style={{ width: '100%' }}
        >
          {availableInheritOptions.map(groupName => (
            <Select.Option key={groupName} value={groupName}>
              {groupName}
            </Select.Option>
          ))}
        </Select>
      </Form.Item>

      <div style={{ textAlign: 'right' }}>
        <Space>
          <Button onClick={onCancel}>
            {t('config.permissionsConfig.cancel')}
          </Button>
          <Button type="primary" onClick={handleOk}>
            {t('config.permissionsConfig.ok')}
          </Button>
        </Space>
      </div>
    </Form>
  );
};

// 玩家编辑表单组件
interface PlayerEditFormProps {
  playerName: string;
  playerData?: Player;
  availableGroups: string[];
  onSave: (name: string, data: Player) => void;
  onCancel: () => void;
}

const PlayerEditForm: React.FC<PlayerEditFormProps> = ({ playerName, playerData, availableGroups, onSave, onCancel }) => {
  const { t } = useTranslation();
  const [name, setName] = useState(playerName || '');
  const [groups, setGroups] = useState<string[]>(playerData?.groups || []);
  const [permissions, setPermissions] = useState<string[]>(playerData?.permissions || []);

  const handleSave = async () => {
    if (!name.trim()) {
      message.error(t('config.permissionsConfig.playerNameRequired'));
      return;
    }
    await onSave(name.trim(), {
      groups,
      permissions,
    });
  };

  return (
    <Form layout="vertical">
      <Form.Item label={t('config.permissionsConfig.playerName')} required>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t('config.permissionsConfig.playerNamePlaceholder')}
        />
      </Form.Item>

      <Form.Item label={t('config.permissionsConfig.groups')}>
        <Select
          mode="multiple"
          value={groups}
          onChange={setGroups}
          placeholder={t('config.permissionsConfig.groupsPlaceholder')}
          style={{ width: '100%' }}
        >
          {availableGroups.map(groupName => (
            <Option key={groupName} value={groupName}>{groupName}</Option>
          ))}
        </Select>
      </Form.Item>

      <Form.Item label={t('config.permissionsConfig.permissions')}>
        <div style={{ marginBottom: 8 }}>
          {permissions.map((permission, index) => (
            <Tag
              key={index}
              closable
              onClose={() => setPermissions(permissions.filter((_, i) => i !== index))}
              style={{ marginBottom: 4 }}
            >
              {permission}
            </Tag>
          ))}
        </div>
        <Input
          placeholder={t('config.permissionsConfig.permissionPlaceholder')}
          onPressEnter={(e) => {
            const value = (e.target as HTMLInputElement).value.trim();
            if (value && !permissions.includes(value)) {
              setPermissions([...permissions, value]);
              (e.target as HTMLInputElement).value = '';
            }
          }}
        />
      </Form.Item>

      <div style={{ textAlign: 'right' }}>
        <Space>
          <Button onClick={onCancel}>
            {t('config.permissionsConfig.cancel')}
          </Button>
          <Button type="primary" onClick={async () => await handleSave()}>
            {t('config.permissionsConfig.save')}
          </Button>
        </Space>
      </div>
    </Form>
  );
};

export default PermissionsConfig; 